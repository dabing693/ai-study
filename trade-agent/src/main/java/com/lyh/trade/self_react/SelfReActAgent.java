package com.lyh.trade.self_react;

import com.alibaba.fastjson2.JSONArray;
import com.lyh.trade.self_react.domain.ChatRequest;
import com.lyh.trade.self_react.domain.ChatResponse;
import com.lyh.trade.self_react.domain.message.AssistantMessage;
import com.lyh.trade.self_react.domain.message.ToolMessage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfReActAgent {
    @Value("${api.key:62d5b9049126430f9255d00f7a72c91e.qa240op6bmKv3Axq}")
    private String apiKey;
    public static final String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private final RestTemplate restTemplate;
    @Resource
    private ToolBuilder toolBuilder;

    public ChatResponse chat(String query, String sessionId) {
        ChatRequest request = ChatRequest.userMessage(query).addTool(toolBuilder.getTools());
        ChatResponse response = call(request);
        while (response.hasToolCalls()) {
            //添加模型返回的assistant消息
            request.addMessage(response.getMessage());
            List<AssistantMessage.ToolCall> toolCalls = response.getToolCalls();
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                ToolMessage toolMessage = ToolInvoker.invoke(toolCall);
                //添加工具调用的tool消息
                request.addMessage(toolMessage);
            }
            response = call(request);
        }
        return response;
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + apiKey);
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(url, httpEntity, ChatResponse.class);
    }
}
