package com.lyh.base.agent.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.domain.FunctionTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MCP 客户端，使用 RestTemplate 与 MCP 服务器通信
 */
@Slf4j
public class McpRestTemplateClient {
    //参考McpServerSseProperties
    private static final String MCP_SSE_ENDPOINT = "/sse";
    private static final String MCP_MESSAGE_ENDPOINT = "/mcp/message";
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private WebClient sseWebClient;
    private Disposable sseSubscription;
    private String pathWithSessionId;
    private boolean initialized = false;
    // JSON-RPC 请求 ID 生成器
    private AtomicLong requestId = new AtomicLong(1);
    private ConcurrentHashMap<Long, String> sseDataMap = new ConcurrentHashMap<>();

    public McpRestTemplateClient(String baseUrl) {
        this.baseUrl = baseUrl;
        // 配置 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    public void init() {
        try {
            if (connect()) {
                initialize();
            }
        } catch (Exception e) {
            log.error("初始化mcp客户端失败：{}", baseUrl, e);
        }
    }

    public void destroy() {
        close();
    }

    /**
     * 建立 SSE 连接并获取 Session ID
     */
    public boolean connect() throws Exception {
        log.info("正在建立SSE链接：{}...", baseUrl);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        // 使用 WebClient 建立 SSE 连接
        sseWebClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        sseSubscription = sseWebClient.get()
                .uri(MCP_SSE_ENDPOINT)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(sub -> log.info("SSE已连接"))
                .doOnNext(data -> {
                    if (pathWithSessionId == null && data.startsWith(MCP_MESSAGE_ENDPOINT)) {
                        pathWithSessionId = data;
                    }
                    log.info("SSE数据：{}", data);
                    try {
                        JSONObject jsonObject = JSON.parseObject(data);
                        Long id = jsonObject.getLong("id");
                        if (sseDataMap.containsKey(id)) {
                            log.warn("数据已存在：{}", id);
                        }
                        sseDataMap.put(id, data);
                    } catch (Exception e) {

                    }
                    latch.countDown();
                })
                .doOnError(error -> {
                    log.error("SSE错误：{}", error.getMessage());
                    errorRef.set(error);
                    latch.countDown();
                })
                .doOnCancel(() -> log.info("SSE已取消"))
                .subscribe();
        // 等待SSE连接建立或出现错误
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                log.error("SSE连接超时");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("SSE连接被中断");
            return false;
        }
        if (errorRef.get() != null) {
            log.error("SSE连接失败：{}", errorRef.get());
            return false;
        }

        if (pathWithSessionId == null) {
            log.error("获取session ID失败");
            return false;
        }
        log.info("成功获取sessionId：{}", pathWithSessionId);
        return true;
    }


    /**
     * 将远端工具注册转为模型的function
     */
    public List<FunctionTool.Function> tools2Functions() {
        List<FunctionTool.Function> functionList = new ArrayList<>();
        if (!initialized) {
            return functionList;
        }
        JSONObject tools = listTools();
        ToolsListResp toolsListResp = tools.to(ToolsListResp.class);
        if (toolsListResp == null || toolsListResp.getResult() == null) {
            return functionList;
        }
        ToolsListResp.Result result = toolsListResp.getResult();
        if (CollectionUtils.isEmpty(result.getTools())) {
            return functionList;
        }
        for (ToolsListResp.Result.Tool tool : result.getTools()) {
            String name = tool.getName();
            String desc = tool.getDescription();
            JSONObject inputSchema = tool.getInputSchema();

            FunctionTool.Function function = new FunctionTool.Function();
            function.setName(name);
            function.setDescription(desc);
            if (inputSchema == null) {
                continue;
            }

            JSONObject properties = inputSchema.getJSONObject("properties");
            JSONArray requiredArgs = inputSchema.getJSONArray("required");

            LinkedHashMap<String, FunctionTool.Function.Parameters.Property> propsMap = new LinkedHashMap<>();
            List<String> requiredList = new ArrayList<>();

            if (requiredArgs != null) {
                for (Object req : requiredArgs) {
                    requiredList.add(req.toString());
                }
            }

            if (properties != null) {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    JSONObject propDef = (JSONObject) entry.getValue();
                    FunctionTool.Function.Parameters.Property prop = new FunctionTool.Function.Parameters.Property();
                    prop.setType(propDef.getString("type"));
                    prop.setDescription(propDef.getString("description"));
                    propsMap.put(entry.getKey(), prop);
                }
            }

            function.setParameters(new FunctionTool.Function.Parameters(propsMap, requiredList));
            functionList.add(function);
        }
        return functionList;
    }

    /**
     * 发送 JSON-RPC 请求
     */
    private JSONObject sendRequest(String method, Object params) {
        try {
            long id = requestId.getAndIncrement();
            JSONObject requestNode = new JSONObject();
            requestNode.put("jsonrpc", "2.0");
            requestNode.put("method", method);
            requestNode.put("id", id);
            if (params != null) {
                requestNode.put("params", params);
            }
            String requestBody = JSON.toJSONString(requestNode);
            log.info("mcp请求：{}", requestBody);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + pathWithSessionId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                if (body == null) {
                    body = deferredGet(id, 10, TimeUnit.SECONDS);
                }
                log.info("mcp响应：{}", body);
                if (!StringUtils.hasText(body)) {
                    return new JSONObject();
                }
                return JSONObject.parseObject(body);
            } else {
                log.error("Http错误：{}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("发送mcp请求失败", e);
            return null;
        }
    }

    private String deferredGet(Long id, int timeout, TimeUnit timeUnit) {
        long timeoutMillis = timeUnit.toMillis(timeout);
        long start = System.currentTimeMillis();
        while (true) {
            if (sseDataMap.containsKey(id)) {
                return sseDataMap.remove(id);
            }
            //超时终止
            if (System.currentTimeMillis() - start > timeoutMillis) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    /**
     * 初始化 MCP 连接
     */
    public boolean initialize() {
        log.info("mcp客户端开始初始化...");
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new HashMap<>());
        Map<String, String> clientInfo = new HashMap<>();
        clientInfo.put("name", "java-resttemplate-client");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        JSONObject response = sendRequest(McpMethod.INITIALIZE.getName(), params);
        if (response != null) {
            log.info("mcp客户端初始化成功");
            this.initialized = true;
            sendInitialized();
            return true;
        } else {
            log.error("mcp客户端初始化失败");
        }
        return false;
    }

    private void sendInitialized() {
        log.info("开始发送已初始化通知...");
        try {
            JSONObject requestNode = new JSONObject();
            requestNode.put("jsonrpc", "2.0");
            requestNode.put("method", McpMethod.NOTIFICATIONS_INITIALIZED.getName());
            requestNode.put("params", new JSONObject());

            String requestBody = JSON.toJSONString(requestNode);
            HttpEntity<String> entity = new HttpEntity<>(requestBody);

            String url = baseUrl + pathWithSessionId;
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("成功发送已初始化通知");
        } catch (Exception e) {
            log.error("发送已初始化通知失败", e);
        }
    }

    public JSONObject listTools() {
        if (!initialized) {
            log.info("客户端还未初始化，请先初始化");
            return null;
        }
        log.info("开始获取tool list");
        return sendRequest(McpMethod.TOOLS_LIST.getName(), null);
    }

    public JSONObject callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized) {
            log.info("客户端还未初始化，请先初始化");
            return null;
        }
        log.info("开始调用工具：{}", toolName);
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);
        return sendRequest(McpMethod.TOOLS_CALL.getName(), params);
    }

    public JSONObject listResources() {
        if (!initialized) {
            log.info("客户端还未初始化，请先初始化");
            return null;
        }
        log.info("开始获取资源...");
        return sendRequest(McpMethod.RESOURCES_LIST.getName(), null);
    }

    public void close() {
        if (sseSubscription != null && !sseSubscription.isDisposed()) {
            sseSubscription.dispose();
        }
        log.info("mcp连接关闭");
    }
}
