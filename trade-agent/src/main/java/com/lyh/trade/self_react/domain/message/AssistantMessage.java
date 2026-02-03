package com.lyh.trade.self_react.domain.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class AssistantMessage extends Message {
    @JsonProperty(value = "tool_calls")
    private List<ToolCall> toolCalls;

    public AssistantMessage() {

    }

    public AssistantMessage(String content) {
        super("assistant", content);
    }

    @Data
    public static class ToolCall {
        private Integer index;
        private String id;
        private String type;
        private Function function;

        @Data
        public static class Function {
            private String name;
            private String arguments;
        }
    }
}
