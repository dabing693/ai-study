package com.lyh.base.agent.model.chat.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import com.lyh.base.agent.domain.*;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.exception.ModelRetryException;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * IflowChatModel调用具体实现
 */
@Slf4j
public class IflowChatModel extends ChatModel {

    public IflowChatModel(ChatModelProperty chatModelProperty,
                          RestTemplate restTemplate) {
        super(chatModelProperty, restTemplate);
    }

    @LangfuseObserver
    @Override
    public ChatResponse call(List<Message> messages, List<FunctionTool> tools) {
        // 构造对话请求
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(chatModelProperty.getModelName());
        request.setEnableThinking(chatModelProperty.getEnableThinking());
        if (!CollectionUtils.isEmpty(tools)) {
            request.setTools(tools);
        }
        // 调用模型
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
        for (Integer i = 0; i < chatModelProperty.getRetryNum(); i++) {
            try {
                return streamCall(request, eventConsumer);
            } catch (ModelRetryException e) {
            }
            try {
                TimeUnit.SECONDS.sleep(chatModelProperty.getRetryIntervalSeconds());
            } catch (InterruptedException e) {
                log.error("sleep被中断");
            }
        }
        return StreamChatResult.failResult();
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + chatModelProperty.getApiKey());
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(chatModelProperty.getBaseUrl(), httpEntity, ChatResponse.class);
    }

    private StreamChatResult streamCall(ChatRequest request, Consumer<StreamEvent> eventConsumer) {
        String requestBody = JSONObject.toJSONString(request);
        log.info("Iflow请求: {}", requestBody);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatModelProperty.getBaseUrl()))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + chatModelProperty.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        StringBuilder totalBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        StringBuilder toolCallsBuilder = new StringBuilder();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<java.util.stream.Stream<String>> response = client.send(httpRequest,
                    HttpResponse.BodyHandlers.ofLines());
            response.body().forEach(line -> handleStreamLine(line, eventConsumer, totalBuilder,
                    contentBuilder, reasoningBuilder, toolCallsBuilder));
        } catch (Exception e) {
            log.error("流式输出异常", e);
            throw new RuntimeException(e);
        }
        // log.info("content结果：{}", contentBuilder);
        // log.info("reasoning结果：{}", reasoningBuilder);
        // log.info("toolCalls结果：{}", toolCallsBuilder);
        boolean allEmpty = contentBuilder.isEmpty() && reasoningBuilder.isEmpty() && toolCallsBuilder.isEmpty();
        if (allEmpty) {
            log.error("模型未输出任何内容：\n{}", totalBuilder);
            throw new ModelRetryException("模型未输出任何内容");
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

    private void handleStreamLine(String line,
                                  Consumer<StreamEvent> eventConsumer,
                                  StringBuilder totalBuilder,
                                  StringBuilder contentBuilder,
                                  StringBuilder reasoningBuilder,
                                  StringBuilder toolCallsBuilder) {
        totalBuilder.append(line).append("\n[SEP]\n");
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
            log.error("流式输出异常：{}", line);
            throw new RuntimeException(ex);
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

        String deltaReasoning = Optional.ofNullable(delta.getReasoningContent()).orElse("");
        if (StringUtils.hasLength(deltaReasoning)) {
            reasoningBuilder.append(deltaReasoning);
            eventConsumer.accept(StreamEvent.reasoningDelta(deltaReasoning));
        }
        String deltaContent = Optional.ofNullable(delta.getContent()).orElse("");
        if (StringUtils.hasLength(deltaContent)) {
            contentBuilder.append(deltaContent);
            eventConsumer.accept(StreamEvent.delta(deltaContent));
        }
        String toolCalls = delta.getToolCalls();
        try {
            JSONArray tmpArray = JSONArray.parseArray(toolCalls);
            if (tmpArray != null && tmpArray.size() > 0) {
                log.info("解析工具为JSONArray成功且非空，直接append：{}", toolCalls);
                toolCallsBuilder.append(toolCalls);
            }
        } catch (Exception ex) {
            log.error("解析工具为JSONArray失败，直接append：{}", toolCalls);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                toolCallsBuilder.append(toolCalls);
            }
        }
    }

    private List<AssistantMessage.ToolCall> parseToolCalls(String toolCallsStr) {
        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        if (!StringUtils.hasLength(toolCallsStr)) {
            return result;
        }
        String normalized = toolCallsStr.replaceAll("\\]\\s*\\[", ",");
        List<AssistantMessage.ToolCall> rawCalls;
        try {
            rawCalls = JSONArray.parseArray(normalized, AssistantMessage.ToolCall.class);
        } catch (Exception e) {
            log.warn("Failed to parse tool_calls in Iflow: {}", toolCallsStr, e);
            return result;
        }
        Map<Integer, AssistantMessage.ToolCall> mergedMap = new LinkedHashMap<>();
        for (AssistantMessage.ToolCall raw : rawCalls) {
            int idx = raw.getIndex() != null ? raw.getIndex() : 0;
            AssistantMessage.ToolCall merged = mergedMap.get(idx);
            if (merged == null) {
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
