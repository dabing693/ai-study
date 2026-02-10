package com.lyh.base.agent.domain;

import com.alibaba.fastjson2.JSONArray;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.ToolMessage;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/8
 */
@Data
public class StreamEvent {
    private String type;
    private String role;
    private String content;
    private String reasoningContent;
    private String toolCalls;
    private String conversationId;
    private Integer assistantIndex;

    public static StreamEvent assistantStart(Integer index) {
        StreamEvent event = new StreamEvent();
        event.type = "assistant_start";
        event.role = "assistant";
        event.assistantIndex = index;
        return event;
    }

    public static StreamEvent delta(String content) {
        StreamEvent event = new StreamEvent();
        event.type = "delta";
        event.role = "assistant";
        event.content = content;
        return event;
    }

    public static StreamEvent reasoningDelta(String reasoningContent) {
        StreamEvent event = new StreamEvent();
        event.type = "reasoning_delta";
        event.role = "assistant";
        event.reasoningContent = reasoningContent;
        return event;
    }

    public static StreamEvent toolCalls(List<AssistantMessage.ToolCall> toolCalls) {
        StreamEvent event = new StreamEvent();
        event.type = "tool_call";
        event.role = "assistant";
        event.toolCalls = JSONArray.toJSONString(toolCalls);
        return event;
    }

    public static StreamEvent toolResult(ToolMessage toolMessage) {
        StreamEvent event = new StreamEvent();
        event.type = "tool_result";
        event.role = "tool";
        event.content = toolMessage != null ? toolMessage.getContent() : null;
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
