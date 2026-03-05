package com.lyh.trade;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MCP 客户端，使用 RestTemplate 与 MCP 服务器通信
 */
public class McpRestTemplateClient {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private WebClient sseWebClient;
    private Disposable sseSubscription;
    private String pathWithSessionId;
    private boolean initialized = false;

    // JSON-RPC 请求 ID 生成器
    private int requestId = 1;

    public McpRestTemplateClient(String baseUrl) {
        this.baseUrl = baseUrl;

        // 配置 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);

        // 配置 ObjectMapper
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 建立 SSE 连接并获取 Session ID
     */
    public boolean connect() throws Exception {
        System.out.println("📡 Establishing SSE connection...");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> sessionIdRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // 使用 WebClient 建立 SSE 连接
        sseWebClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        sseSubscription = sseWebClient.get()
                .uri("/sse")
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(sub -> System.out.println("SSE connected"))
                .doOnNext(data -> {
                    if (pathWithSessionId == null && data.startsWith("/mcp/message")) {
                        pathWithSessionId = data;
                    }
                    System.out.println("SSE data: " + data);
                    // 尝试从数据中解析sessionId，如果有的话
                    // 在第一次接收到数据时释放latch
                    latch.countDown();
                })
                .doOnError(error -> {
                    System.err.println("SSE error: " + error);
                    errorRef.set(error);
                    latch.countDown(); // 出现错误时也要释放latch
                })
                .doOnCancel(() -> System.out.println("SSE cancelled"))
                .subscribe();

        // 等待SSE连接建立或出现错误
        try {
            // 等待最多10秒让连接建立
            if (!latch.await(10, TimeUnit.SECONDS)) {
                System.err.println("❌ SSE connection timeout");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("❌ SSE connection interrupted");
            return false;
        }

        if (errorRef.get() != null) {
            System.err.println("❌ SSE connection failed: " + errorRef.get());
            return false;
        }


        if (pathWithSessionId == null) {
            System.err.println("❌ Failed to get session ID");
            return false;
        }

        this.pathWithSessionId = pathWithSessionId;
        System.out.println("✅ Got session ID: " + pathWithSessionId);
        return true;
    }

    /**
     * 发送 JSON-RPC 请求
     */
    private JsonNode sendRequest(String method, Object params) {
        return sendRequest(method, params, requestId++);
    }

    /**
     * 发送 JSON-RPC 请求（指定 ID）
     */
    private JsonNode sendRequest(String method, Object params, int id) {
        try {
            // 构建 JSON-RPC 请求
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("jsonrpc", "2.0");
            requestNode.put("method", method);
            requestNode.put("id", id);

            if (params != null) {
                requestNode.set("params", objectMapper.valueToTree(params));
            }

            String requestBody = objectMapper.writeValueAsString(requestNode);
            System.out.println("📤 Request: " + requestBody);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //headers.set("Mcp-Session-Id", sessionId); // 在请求头中传递sessionId

            // 创建 HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(requestBody);

            // 发送请求
            String url = baseUrl + pathWithSessionId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("📥 Response: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                if (!StringUtils.hasText(response.getBody())) {
                    body = "{}";
                }
                return objectMapper.readTree(body);
            } else {
                System.err.println("HTTP Error: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error sending request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 初始化 MCP 连接
     */
    public boolean initialize() {
        System.out.println("🔄 Initializing...");

        // 构建初始化参数
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", new HashMap<>());

        Map<String, String> clientInfo = new HashMap<>();
        clientInfo.put("name", "java-resttemplate-client");
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);

        JsonNode response = sendRequest("initialize", params, 1);

        if (response != null) {
            System.out.println("✅ Initialize successful");
            this.initialized = true;

            // 发送 initialized 通知
            sendInitialized();
            return true;
        } else {
            System.err.println("❌ Initialize failed: " + response.get("error"));
        }

        return false;
    }

    /**
     * 发送 initialized 通知
     */
    private void sendInitialized() {
        System.out.println("📨 Sending initialized notification...");

        // 通知不需要 ID，也不需要响应
        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("jsonrpc", "2.0");
            // requestNode.put("method", "initialized");
            requestNode.put("method", "notifications/initialized");
            requestNode.set("params", objectMapper.createObjectNode());

            String requestBody = objectMapper.writeValueAsString(requestNode);

            HttpEntity<String> entity = new HttpEntity<>(requestBody);

            String url = baseUrl + pathWithSessionId;
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            System.out.println("✅ Initialized notification sent");
        } catch (Exception e) {
            System.err.println("Error sending initialized notification: " + e.getMessage());
        }
    }

    /**
     * 获取工具列表
     */
    public JsonNode listTools() {
        if (!initialized) {
            System.err.println("❌ Client not initialized. Call initialize() first.");
            return null;
        }

        System.out.println("🔧 Getting tools list...");
        return sendRequest("tools/list", null, 2);
    }

    /**
     * 调用工具
     */
    public JsonNode callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized) {
            System.err.println("❌ Client not initialized. Call initialize() first.");
            return null;
        }

        System.out.println("🛠️ Calling tool: " + toolName);

        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        return sendRequest("tools/call", params);
    }

    /**
     * 获取资源列表
     */
    public JsonNode listResources() {
        if (!initialized) {
            System.err.println("❌ Client not initialized. Call initialize() first.");
            return null;
        }

        System.out.println("📁 Getting resources list...");
        return sendRequest("resources/list", null);
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (sseSubscription != null && !sseSubscription.isDisposed()) {
            sseSubscription.dispose();
        }
        System.out.println("👋 Connection closed");
    }

    // Getter
    public String getPathWithSessionId() {
        return pathWithSessionId;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setSessionId(String sessionId) {
        this.pathWithSessionId = "/mcp/message?sessionId=" + sessionId;
    }
}