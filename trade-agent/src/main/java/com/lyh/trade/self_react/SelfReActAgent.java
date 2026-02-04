package com.lyh.trade.self_react;

import com.alibaba.fastjson2.JSONArray;
import com.lyh.trade.self_react.domain.ChatRequest;
import com.lyh.trade.self_react.domain.ChatResponse;
import com.lyh.trade.self_react.domain.message.AssistantMessage;
import com.lyh.trade.self_react.domain.message.Message;
import com.lyh.trade.self_react.domain.message.ToolMessage;
import com.lyh.trade.tools.DateTimeTool;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelfReActAgent {
    private static final String system_prompt = """
            你是一个乐于助人的金融领域专家，你善于利用各种工具解决用户的问题。
            当前时间：{cur_date_time}
            """;
    @Value("${api.key:62d5b9049126430f9255d00f7a72c91e.qa240op6bmKv3Axq}")
    private String apiKey;
    @Value("${glm.model:glm-4.5-flash}")
    private String model;
    @Value("${max.message.num:20}")
    private Integer maxMessageNum;
    public static final String url = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private final RestTemplate restTemplate;
    @Resource
    private ToolBuilder toolBuilder;
    @Resource
    private MysqlMemory mysqlMemory;
    @Resource
    private DateTimeTool dateTimeTool;

    public ChatResponse chat(String query) {
        //历史消息
        List<Message> hisMessages = mysqlMemory.get(RequestContext.getSession(), maxMessageNum);
        ChatRequest request = ChatRequest.initMessage(systemPrompt(), hisMessages, query, maxMessageNum)
                .model(model)
                .enableThinking(false)
                .addTool(toolBuilder.getTools());

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
        //后处理
        postHandle(request, response);
        return response;
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + apiKey);
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(url, httpEntity, ChatResponse.class);
    }

    private void postHandle(ChatRequest request, ChatResponse response) {
        List<Message> totalMsg = request.getMessages().stream()
                //非历史消息才保存
                .filter(it -> !it.isHis())
                .collect(Collectors.toList());
        totalMsg.add(response.getMessage());
        mysqlMemory.addAll(totalMsg);
    }

    private String systemPrompt() {
        return system_prompt.replace("{cur_date_time}", dateTimeTool.currentDateTime());
    }
}
