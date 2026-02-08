package com.lyh.finance.domain;

import com.lyh.finance.domain.message.AssistantMessage;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/8
 */
@Data
public class StreamChatResult {
    private AssistantMessage message;

    public boolean hasToolCalls() {
        return !CollectionUtils.isEmpty(getToolCalls());
    }

    public List<AssistantMessage.ToolCall> getToolCalls() {
        if (message == null || CollectionUtils.isEmpty(message.getToolCalls())) {
            return new ArrayList<>();
        }
        return message.getToolCalls();
    }
}
