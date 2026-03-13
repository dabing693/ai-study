package com.lyh.base.agent.domain;

import com.lyh.base.agent.domain.message.AssistantMessage;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/8
 */
@Data
public class StreamChatResult {
    private AssistantMessage message;

    public boolean hasToolCalls() {
        return message != null && message.hasToolCalls();
    }

    public List<AssistantMessage.ToolCall> getToolCalls() {
        return message == null ? Collections.emptyList() : message.getToolCalls();
    }

    public static StreamChatResult failResult() {
        StreamChatResult result = new StreamChatResult();
        AssistantMessage msg = new AssistantMessage("调用模型api达到最大重试次数，依然失败");
        result.setMessage(msg);
        return result;
    }
}
