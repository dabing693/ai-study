package com.lyh.finance.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.finance.domain.message.AssistantMessage;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
public class ChatResponse {
    public List<Choice> choices;
    /**
     * 时间戳通常使用 long类型
     */
    public long created;
    public String id;
    public String model;
    public String object;
    @JsonProperty(value = "request_id")
    public String requestId;
    public Usage usage;

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

    public List<AssistantMessage.ToolCall> getToolCalls() {
        return Optional.ofNullable(this.choices)
                .filter(it -> !CollectionUtils.isEmpty(it))
                .map(it -> it.get(0))
                .map(Choice::getMessage)
                .map(AssistantMessage::getToolCalls)
                .orElse(new ArrayList<>());
    }

    public AssistantMessage getMessage() {
        return Optional.ofNullable(this.choices)
                .filter(it -> !CollectionUtils.isEmpty(it))
                .map(it -> it.get(0))
                .map(Choice::getMessage)
                .orElse(null);
    }
}