package com.lyh.base.agent.mcp;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tool.ToolResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@ConditionalOnProperty(name = "mcp.server.urls")
public class McpClientManager {

    @Value("${mcp.server.urls:}")
    private String serverUrls;

    private final List<McpRestTemplateClient> clients = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(serverUrls)) {
            return;
        }
        String[] urls = serverUrls.split(",");
        for (String url : urls) {
            String cleanUrl = url.trim();
            if (StringUtils.hasText(cleanUrl)) {
                McpRestTemplateClient client = new McpRestTemplateClient(cleanUrl);
                client.init();
                clients.add(client);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        for (McpRestTemplateClient client : clients) {
            client.destroy();
        }
    }

    public void registerAllTools(ToolBuilder toolBuilder) {
        for (McpRestTemplateClient client : clients) {
            registerOneClient(client, toolBuilder);
        }
    }

    private void registerOneClient(McpRestTemplateClient client, ToolBuilder toolBuilder) {
        List<FunctionTool.Function> functionList = client.tools2Functions();
        for (FunctionTool.Function function : functionList) {
            String name = function.getName();
            // 注册动态回调
            toolBuilder.addDynamicTool(function,
                    argsMap -> callback(client, name, argsMap));
        }
    }

    private Object callback(McpRestTemplateClient client, String name, Map<String, Object> argsMap) {
        JSONObject callRes = client.callTool(name, argsMap);
        ToolsCallResp toolsCallResp = callRes.to(ToolsCallResp.class);
        List<ToolsCallResp.Result.Content> contents = Optional.ofNullable(toolsCallResp).
                map(ToolsCallResp::getResult)
                .map(ToolsCallResp.Result::getContent)
                .orElse(null);
        if (contents == null || contents.isEmpty()) {
            return "调用工具失败" + name;
        }
        String text = contents.get(0).getText();
        try {
            ToolResult result = JSONObject.parseObject(text, ToolResult.class);
            return result;
        } catch (Exception e) {
            log.info("非ToolResult类型响应：{}，异常：{}", name, e.getMessage());
            return removeEscape(text);
        }
    }

    private String removeEscape(String text) {
        if (text != null) {
            text = text.trim();
            //去掉首尾双引号 将转义的\n替换为真正的换行符
            text = text.replaceAll("^\"|\"$", "").replace("\\n", "\n");
        }
        log.info("mcp工具调用结果：\n{}", text);
        return text;
    }
}
