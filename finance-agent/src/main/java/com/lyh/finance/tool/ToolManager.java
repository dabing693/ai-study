package com.lyh.finance.tool;

import com.lyh.finance.domain.FunctionTool;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.ToolMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@RequiredArgsConstructor
@Component
public class ToolManager {
    private final ToolBuilder toolBuilder;

    public List<FunctionTool> getTools() {
        return toolBuilder.getTools();
    }

    public ToolMessage invoke(AssistantMessage.ToolCall toolCall) {
        return ToolInvoker.invoke(toolCall);
    }
}
