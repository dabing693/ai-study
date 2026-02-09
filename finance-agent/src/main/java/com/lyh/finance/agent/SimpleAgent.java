package com.lyh.finance.agent;

import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.domain.message.UserMessage;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public abstract class SimpleAgent extends BaseAgent {
    public SimpleAgent(ChatModel chatModel,
                       ToolManager toolManager
    ) {
        super(chatModel, null, toolManager);
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
