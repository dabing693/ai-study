package com.lyh.finance.agent;

import com.lyh.finance.agent.property.AgentProperty;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseAgent<PROPERTY extends AgentProperty> {
    protected final ChatModel chatModel;
    protected final MemoryManager memoryManager;
    protected final ToolManager toolManager;
    protected final PROPERTY agentProperty;

    public String send(String query) {
        return chat(query).getReply();
    }

    public abstract ChatResponse chat(String query);

    public abstract List<Message> sense(String query);

    public abstract ChatResponse plan(List<Message> messageList);

    public abstract List<Message> action(List<AssistantMessage.ToolCall> toolCalls);

    public SystemMessage systemMessage() {
        try {
            InputStreamReader reader = new InputStreamReader(
                    agentProperty.getPromptFile().getInputStream(), StandardCharsets.UTF_8);
            String template = FileCopyUtils.copyToString(reader);
            return new SystemMessage(template);
        } catch (Exception e) {
            log.error("获取系统提示词失败");
        }
        return null;
    }

    protected void addAndSave(List<Message> messages, Message message) {
        if (message == null) {
            throw new RuntimeException("消息为空");
        }
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
