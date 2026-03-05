package com.lyh.base.agent.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.base.agent.domain.message.AssistantMessage;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Data
public class ChatResponse {
    private List<Choice> choices;
    /**
     * 时间戳通常使用 long类型
     */
    private long created;
    private String id;
    private String model;
    private String object;
    @JsonProperty(value = "request_id")
    private String requestId;
    private Usage usage;

    /**
     * 选择结果
     */
    @Data
    public static class Choice {
        @JsonProperty(value = "finish_reason")
        public String finishReason;
        public int index;
        public AssistantMessage message;
    }

    /**
     * Token消耗情况
     */
    @Data
    public static class Usage {
        @JsonProperty(value = "completion_tokens")
        public int completionTokens;
        @JsonProperty(value = "prompt_tokens")
        public int promptTokens;
        @JsonProperty(value = "total_tokens")
        public int totalTokens;
    }

    public boolean hasToolCalls() {
        return !CollectionUtils.isEmpty(getToolCalls());
    }

    @JsonIgnore
    public List<AssistantMessage.ToolCall> getToolCalls() {
        return Optional.ofNullable(this.choices)
                .filter(it -> !CollectionUtils.isEmpty(it))
                .map(it -> it.get(0))
                .map(Choice::getMessage)
                .map(AssistantMessage::getToolCalls)
                .orElse(new ArrayList<>());
    }

    @JsonIgnore
    public AssistantMessage getMessage() {
        return Optional.ofNullable(this.choices)
                .filter(it -> !CollectionUtils.isEmpty(it))
                .map(it -> it.get(0))
                .map(Choice::getMessage)
                .orElse(null);
    }

    @JsonIgnore
    public String getReply() {
        return Optional.ofNullable(this.choices)
                .filter(it -> !CollectionUtils.isEmpty(it))
                .map(it -> it.get(0))
                .map(Choice::getMessage)
                .map(AssistantMessage::getContent)
                .orElse(null);
    }
}