package com.lyh.finance.agent;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@RequiredArgsConstructor
public abstract class BaseAgent {
    protected final ChatModel chatModel;
    protected final MemoryManager memoryManager;
    protected final ToolManager toolManager;

    public abstract ChatResponse chat(String query);

    public abstract List<Message> sense(String query);

    public abstract ChatResponse plan(List<Message> messageList);

    public abstract List<Message> action(List<AssistantMessage.ToolCall> toolCalls);

    public abstract SystemMessage systemMessage();

    protected void addAndSave(List<Message> messages, Message message) {
        addAndSave(messages, List.of(message));
    }

    protected void addAndSave(List<Message> messages, List<Message> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }
        messages.addAll(newMessages);
        memoryManager.saveMemory(newMessages);
    }
}
