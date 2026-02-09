package com.lyh.finance.agent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
public abstract class BaseAgent {
    protected final ChatModel chatModel;
    protected final MemoryManager memoryManager;
    protected final ToolManager toolManager;

    public String send(String query) {
        return chat(query).getReply();
    }

    public abstract ChatResponse chat(String query);

    public abstract List<Message> sense(String query);

    public abstract ChatResponse plan(List<Message> messageList);

    public abstract List<Message> action(List<AssistantMessage.ToolCall> toolCalls);

    public SystemMessage systemMessage() {
        try {
            String promptFile = PropertyNamingStrategies.KEBAB_CASE.nameForField(null, null,
                    this.getClass().getSimpleName().replace("Agent", "")) + ".txt";
            Resource promptResource = new ClassPathResource("prompt/" + promptFile);
            InputStreamReader reader = new InputStreamReader(
                    promptResource.getInputStream(), StandardCharsets.UTF_8);
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
