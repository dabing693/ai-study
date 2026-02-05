package com.lyh.finance.domain.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.finance.enums.MessageType;
import lombok.Data;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class ToolMessage extends Message {
    @JsonProperty(value = "tool_call_id")
    private String toolCallId;

    public ToolMessage(String content, String toolCallId) {
        super(MessageType.tool.name(), content);
        this.toolCallId = toolCallId;
    }

    public static String strContent(Object content) {
        String strContent = null;
        if (content instanceof String) {
            strContent = (String) content;
        } else {
            throw new RuntimeException("未支持的类型！");
        }
        return strContent;
    }
}
