package com.lyh.finance.model.chat.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import com.lyh.finance.domain.ChatRequest;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.FunctionTool;
import com.lyh.finance.domain.StreamChatResult;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.model.chat.property.ChatModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
public class ZhiPuChatModel extends ChatModel {


    public ZhiPuChatModel(ChatModelProperty chatModelProperty,
                          RestTemplate restTemplate) {
        super(chatModelProperty, restTemplate);
    }

    @Override
    public ChatResponse call(List<Message> messages, List<FunctionTool> tools) {
        //构造对话请求
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(chatModelProperty.getModelName());
        request.setEnableThinking(chatModelProperty.getEnableThinking());
        if (!CollectionUtils.isEmpty(tools)) {
            request.setTools(tools);
        }
        //调用模型
        return call(request);
    }

    @Override
    public StreamChatResult stream(List<Message> messages,
                                   List<FunctionTool> tools,
                                   Consumer<String> onDelta) {
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(chatModelProperty.getModelName());
        request.setEnableThinking(chatModelProperty.getEnableThinking());
        request.setStream(true);
        if (!CollectionUtils.isEmpty(tools)) {
            request.setTools(tools);
        }
        return streamCall(request, onDelta);
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + chatModelProperty.getApiKey());
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(chatModelProperty.getBaseUrl(), httpEntity, ChatResponse.class);
    }

    private StreamChatResult streamCall(ChatRequest request, Consumer<String> onDelta) {
        String requestBody = JSONObject.toJSONString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatModelProperty.getBaseUrl()))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + chatModelProperty.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        StringBuilder contentBuilder = new StringBuilder();
        Map<Integer, AssistantMessage.ToolCall> toolCallMap = new LinkedHashMap<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.util.stream.Stream<String>> response =
                    client.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            response.body().forEach(line -> handleStreamLine(line, onDelta, contentBuilder, toolCallMap));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Stream call failed", e);
        }

        AssistantMessage message = new AssistantMessage(contentBuilder.toString());
        if (!toolCallMap.isEmpty()) {
            message.setToolCalls(new ArrayList<>(toolCallMap.values()));
        }
        StreamChatResult result = new StreamChatResult();
        result.setMessage(message);
        return result;
    }

    /**
     * 处理模型流式输出chunk，包括追加content和sse推前端，及特殊处理工具调用参数
     *
     * @param line
     * @param onDelta
     * @param contentBuilder
     * @param toolCallMap
     */
    private void handleStreamLine(String line,
                                  Consumer<String> onDelta,
                                  StringBuilder contentBuilder,
                                  Map<Integer, AssistantMessage.ToolCall> toolCallMap) {
        if (!StringUtils.hasLength(line)) {
            return;
        }
        String payload = line.trim();
        if (payload.startsWith(PAYLOAD_PREFIX)) {
            payload = payload.substring(5).trim();
        }
        if (!StringUtils.hasLength(payload) || PAYLOAD_END.equals(payload)) {
            return;
        }
        StreamPayload streamPayload;
        try {
            streamPayload = JSONObject.parseObject(payload, StreamPayload.class);
        } catch (Exception ex) {
            return;
        }
        StreamPayload.Choice.Delta delta = Optional.ofNullable(streamPayload)
                .map(StreamPayload::getChoices)
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .map(choice -> choice.getDelta() != null ? choice.getDelta() : choice.getMessage())
                .orElse(null);
        if (delta == null) {
            return;
        }
        String deltaContent = Optional.ofNullable(delta.getContent()).orElse("");
        if (StringUtils.hasLength(deltaContent)) {
            //追加模型流式输出chunk
            contentBuilder.append(deltaContent);
            //推送模型流式输出chunk
            onDelta.accept(deltaContent);
        }
        List<AssistantMessage.ToolCall> toolCalls = delta.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            mergeToolCalls(toolCalls, toolCallMap);
        }
    }

    /**
     * 合并工具调用参数
     *
     * @param toolCalls
     * @param toolCallMap
     */
    private void mergeToolCalls(List<AssistantMessage.ToolCall> toolCalls,
                                Map<Integer, AssistantMessage.ToolCall> toolCallMap) {
        for (AssistantMessage.ToolCall incoming : toolCalls) {
            if (incoming == null) {
                continue;
            }
            Integer index = incoming.getIndex();
            if (index == null) {
                index = toolCallMap.size();
            }
            AssistantMessage.ToolCall toolCall = toolCallMap.computeIfAbsent(index, k -> new AssistantMessage.ToolCall());
            if (StringUtils.hasLength(incoming.getId())) {
                toolCall.setId(incoming.getId());
            }
            if (StringUtils.hasLength(incoming.getType())) {
                toolCall.setType(incoming.getType());
            }
            AssistantMessage.ToolCall.Function incomingFunction = incoming.getFunction();
            if (incomingFunction != null) {
                AssistantMessage.ToolCall.Function function = toolCall.getFunction();
                if (function == null) {
                    function = new AssistantMessage.ToolCall.Function();
                    toolCall.setFunction(function);
                }
                if (StringUtils.hasLength(incomingFunction.getName())) {
                    function.setName(incomingFunction.getName());
                }
                if (StringUtils.hasLength(incomingFunction.getArguments())) {
                    String current = function.getArguments();
                    function.setArguments(current == null ? incomingFunction.getArguments()
                            : current + incomingFunction.getArguments());
                }
            }
        }
    }

    @Data
    private static class StreamPayload {
        private List<Choice> choices;

        @Data
        private static class Choice {
            private Delta delta;
            private Delta message;

            @Data
            private static class Delta {
                private String content;
                @JSONField(name = "tool_calls")
                private List<AssistantMessage.ToolCall> toolCalls;
            }
        }
    }
}
