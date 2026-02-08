package com.lyh.finance.domain;

import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.ToolMessage;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * @author lengYinHui
 * @date 2026/2/8
 */
@Data
public class StreamEvent {
    private String type;
    private String role;
    private String content;
    private String toolName;
    private String toolArguments;
    private String toolCallId;
    private String conversationId;

    public static StreamEvent delta(String content) {
        StreamEvent event = new StreamEvent();
        event.type = "delta";
        event.role = "assistant";
        event.content = content;
        return event;
    }

    public static StreamEvent toolCall(AssistantMessage.ToolCall toolCall) {
        StreamEvent event = new StreamEvent();
        event.type = "tool_call";
        event.role = "tool";
        if (toolCall != null && toolCall.getFunction() != null) {
            event.toolName = toolCall.getFunction().getName();
            event.toolArguments = toolCall.getFunction().getArguments();
        }
        event.toolCallId = toolCall != null ? toolCall.getId() : null;
        return event;
    }

    public static StreamEvent toolResult(ToolMessage toolMessage) {
        StreamEvent event = new StreamEvent();
        event.type = "tool_result";
        event.role = "tool";
        event.content = toolMessage != null ? toolMessage.getContent() : null;
        event.toolCallId = toolMessage != null ? toolMessage.getToolCallId() : null;
        return event;
    }

    public static StreamEvent session(String conversationId) {
        StreamEvent event = new StreamEvent();
        event.type = "session";
        if (StringUtils.hasLength(conversationId)) {
            event.conversationId = conversationId;
        }
        return event;
    }

    public static StreamEvent done() {
        StreamEvent event = new StreamEvent();
        event.type = "done";
        return event;
    }

    public static StreamEvent error(String message) {
        StreamEvent event = new StreamEvent();
        event.type = "error";
        event.content = message;
        return event;
    }
}
