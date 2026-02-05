package com.lyh.finance.domain.message;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.finance.enums.MessageType;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    public AssistantMessage() {

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
        String reasoningContent = StringUtils.hasLength(this.reasoningContent) ? "思考内容：\n" + this.reasoningContent : "";
        if (StringUtils.hasLength(getContent())) {
            String fullContent=reasoningContent + this.getContent();
            if (CollectionUtils.isEmpty(this.toolCalls)) {
                return fullContent;
            } else {
                return new JSONObject().fluentPut("model_output", fullContent)
                        .fluentPut("tool_calls", JSONArray.toJSONString(this.toolCalls))
                        .toJSONString();
            }
        } else {
            return JSONArray.toJSONString(this.toolCalls);
        }
    }
}
