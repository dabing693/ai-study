package com.lyh.base.agent.model.chat.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import com.lyh.base.agent.domain.*;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.model.chat.property.ChatModelProperty;
import com.lyh.base.agent.observation.LangfuseObserver;
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

    @LangfuseObserver
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

    @LangfuseObserver
    @Override
    public StreamChatResult stream(List<Message> messages,
                                   List<FunctionTool> tools,
                                   Consumer<StreamEvent> eventConsumer) {
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(chatModelProperty.getModelName());
        request.setEnableThinking(chatModelProperty.getEnableThinking());
        request.setStream(true);
        if (!CollectionUtils.isEmpty(tools)) {
            request.setTools(tools);
        }
        return streamCall(request, eventConsumer);
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + chatModelProperty.getApiKey());
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(chatModelProperty.getBaseUrl(), httpEntity, ChatResponse.class);
    }

    private StreamChatResult streamCall(ChatRequest request, Consumer<StreamEvent> eventConsumer) {
        String requestBody = JSONObject.toJSONString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatModelProperty.getBaseUrl()))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + chatModelProperty.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        StringBuilder toolCallsBuilder = new StringBuilder();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.util.stream.Stream<String>> response =
                    client.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            response.body().forEach(line -> handleStreamLine(line, eventConsumer, contentBuilder,
                    reasoningBuilder, toolCallsBuilder));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Stream call failed", e);
        }

        AssistantMessage message = new AssistantMessage(contentBuilder.toString());
        if (StringUtils.hasLength(reasoningBuilder)) {
            message.setReasoningContent(reasoningBuilder.toString());
        }
        if (StringUtils.hasLength(toolCallsBuilder)) {
            String toolCallsStr = toolCallsBuilder.toString();
            List<AssistantMessage.ToolCall> toolCalls = parseToolCalls(toolCallsStr);
            if (!toolCalls.isEmpty()) {
                eventConsumer.accept(StreamEvent.toolCalls(toolCalls));
                message.setToolCalls(toolCalls);
            }
        }
        StreamChatResult result = new StreamChatResult();
        result.setMessage(message);
        return result;
    }

    /**
     * 处理模型流式输出chunk，包括追加content和sse推前端，及特殊处理工具调用参数
     *
     * @param line
     * @param eventConsumer
     * @param contentBuilder
     * @param reasoningBuilder
     * @param toolCallsBuilder
     */
    private void handleStreamLine(String line,
                                  Consumer<StreamEvent> eventConsumer,
                                  StringBuilder contentBuilder,
                                  StringBuilder reasoningBuilder,
                                  StringBuilder toolCallsBuilder) {
        if (!StringUtils.hasLength(line)) {
            return;
        }
        String payload = line.trim();
        if (payload.startsWith(PAYLOAD_PREFIX)) {
            payload = payload.substring(PAYLOAD_PREFIX.length()).trim();
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
        // 处理推理内容 (reasoning_content)
        String deltaReasoning = Optional.ofNullable(delta.getReasoningContent()).orElse("");
        if (StringUtils.hasLength(deltaReasoning)) {
            //追加模型思考chunk
            reasoningBuilder.append(deltaReasoning);
            //推送模型思考chunk
            eventConsumer.accept(StreamEvent.reasoningDelta(deltaReasoning));
            // 可以通过回调通知前端推理内容，如果需要单独展示
        }
        String deltaContent = Optional.ofNullable(delta.getContent()).orElse("");
        if (StringUtils.hasLength(deltaContent)) {
            //追加模型流式输出chunk
            contentBuilder.append(deltaContent);
            //推送模型流式输出chunk
            eventConsumer.accept(StreamEvent.delta(deltaContent));
        }
        String toolCalls = delta.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            toolCallsBuilder.append(toolCalls);
        }
    }

    private List<AssistantMessage.ToolCall> parseToolCalls(String toolCallsStr) {
        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        if (!StringUtils.hasLength(toolCallsStr)) {
            return result;
        }
        // 流式 tool_calls 是多段 JSON 数组拼接而来，形如: [{index:0,...}][{index:0,...}][{index:0,...}]
        // 需要先合并为一个数组，再按 index 分组合并 name 和 arguments
        String normalized = toolCallsStr.replaceAll("\\]\\s*\\[", ",");
        List<AssistantMessage.ToolCall> rawCalls;
        try {
            rawCalls = JSONArray.parseArray(normalized, AssistantMessage.ToolCall.class);
        } catch (Exception e) {
            log.warn("Failed to parse tool_calls: {}", toolCallsStr, e);
            return result;
        }
        // 按 index 分组，合并增量 function.name 和 function.arguments
        Map<Integer, AssistantMessage.ToolCall> mergedMap = new LinkedHashMap<>();
        for (AssistantMessage.ToolCall raw : rawCalls) {
            int idx = raw.getIndex() != null ? raw.getIndex() : 0;
            AssistantMessage.ToolCall merged = mergedMap.get(idx);
            if (merged == null) {
                // 首次出现此 index，直接放入（注意 arguments 可能为空片段）
                merged = new AssistantMessage.ToolCall();
                merged.setIndex(idx);
                merged.setId(raw.getId());
                merged.setType(raw.getType());
                AssistantMessage.ToolCall.Function fn = new AssistantMessage.ToolCall.Function();
                fn.setName(raw.getFunction() != null ? Optional.ofNullable(raw.getFunction().getName()).orElse("") : "");
                fn.setArguments(raw.getFunction() != null ? Optional.ofNullable(raw.getFunction().getArguments()).orElse("") : "");
                merged.setFunction(fn);
                mergedMap.put(idx, merged);
            } else {
                // 后续增量：合并 id、type、name、arguments
                if (raw.getId() != null && merged.getId() == null) {
                    merged.setId(raw.getId());
                }
                if (raw.getType() != null && merged.getType() == null) {
                    merged.setType(raw.getType());
                }
                if (raw.getFunction() != null) {
                    String nameChunk = Optional.ofNullable(raw.getFunction().getName()).orElse("");
                    String argsChunk = Optional.ofNullable(raw.getFunction().getArguments()).orElse("");
                    merged.getFunction().setName(merged.getFunction().getName() + nameChunk);
                    merged.getFunction().setArguments(merged.getFunction().getArguments() + argsChunk);
                }
            }
        }
        result.addAll(mergedMap.values());
        return result;
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
                @JSONField(name = "reasoning_content")
                private String reasoningContent;
                @JSONField(name = "tool_calls")
                private String toolCalls;
            }
        }
    }
}
