package com.lyh.base.agent.domain.message;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.base.agent.enums.MessageType;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class AssistantMessage extends Message {
    @JsonProperty(value = "reasoning_content")
    private String reasoningContent;
    @JsonProperty(value = "tool_calls")
    private List<ToolCall> toolCalls;

    private AssistantMessage() {

    }

    public AssistantMessage(String content) {
        super(MessageType.assistant.name(), content);
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

    public String storedContent() {
        JSONObject jsonObject = new JSONObject();
        if (StringUtils.hasText(this.getContent())) {
            jsonObject.put("content", this.getContent());
        }
        if (StringUtils.hasText(this.getReasoningContent())) {
            jsonObject.put("reasoning_content", this.getReasoningContent());
        }
        if (!CollectionUtils.isEmpty(this.getToolCalls())) {
            jsonObject.put("tool_calls", JSONArray.toJSONString(this.getToolCalls()));
        }
        return jsonObject.toJSONString();
    }

    public boolean hasToolCalls() {
        return !CollectionUtils.isEmpty(this.getToolCalls());
    }
}
