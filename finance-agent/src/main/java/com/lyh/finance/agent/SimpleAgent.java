package com.lyh.finance.agent;

import com.lyh.finance.agent.property.SimpleAgentProperty;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.domain.message.UserMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public class SimpleAgent extends BaseAgent<SimpleAgentProperty> {
    public SimpleAgent(ChatModel chatModel,
                       MemoryManager memoryManager,
                       ToolManager toolManager,
                       SimpleAgentProperty agentProperty
    ) {
        super(chatModel, memoryManager, toolManager, agentProperty);
    }

    @Override
    public ChatResponse chat(String query) {
        SystemMessage systemMessage = systemMessage();
        UserMessage userMessage = new UserMessage(query);
        return chatModel.call(List.of(systemMessage, userMessage));
    }

    @Override
    public List<Message> sense(String query) {
        return null;
    }

    @Override
    public ChatResponse plan(List<Message> messageList) {
        return null;
    }

    @Override
    public List<Message> action(List<AssistantMessage.ToolCall> toolCalls) {
        return null;
    }
}
