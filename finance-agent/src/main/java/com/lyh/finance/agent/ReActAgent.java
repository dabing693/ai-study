package com.lyh.finance.agent;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.StreamChatResult;
import com.lyh.finance.domain.StreamEvent;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.ToolMessage;
import com.lyh.finance.domain.message.UserMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
public abstract class ReActAgent extends BaseAgent {
    public ReActAgent(ChatModel chatModel,
                      MemoryManager memoryManager,
                      ToolManager toolManager
    ) {
        super(chatModel, memoryManager, toolManager);
    }

    private static final int MAX_LOOP_NUM = 10;

    @Override
    public ChatResponse chat(String query) {
        List<Message> messageList = sense(query);
        ChatResponse planResponse = null;
        for (int i = 0; i < getMaxLoopNum(); i++) {
            planResponse = plan(messageList);
            //添加模型返回的assistant消息
            addAndSave(messageList, planResponse.getMessage());
            if (planResponse.hasToolCalls()) {
                //行动，获得tool消息
                List<Message> toolMessages = action(planResponse.getToolCalls());
                //添加工具消息
                addAndSave(messageList, toolMessages);
            } else {
                break;
            }
        }
        return planResponse;
    }

    public void chatStream(String query, Consumer<StreamEvent> eventConsumer) {
        List<Message> messageList = sense(query);
        for (int i = 0; i < getMaxLoopNum(); i++) {
            StreamChatResult streamResult = chatModel.stream(
                    messageList,
                    toolManager.getTools(),
                    delta -> eventConsumer.accept(StreamEvent.delta(delta))
            );
            addAndSave(messageList, streamResult.getMessage());
            if (streamResult.hasToolCalls()) {
                for (AssistantMessage.ToolCall toolCall : streamResult.getToolCalls()) {
                    eventConsumer.accept(StreamEvent.toolCall(toolCall));
                    ToolMessage toolMessage = toolManager.invoke(toolCall);
                    if (toolMessage != null) {
                        addAndSave(messageList, toolMessage);
                        eventConsumer.accept(StreamEvent.toolResult(toolMessage));
                    }
                }
            } else {
                break;
            }
        }
    }

    /**
     * 感知
     *
     * @param query
     * @return
     */
    @Override
    public List<Message> sense(String query) {
        List<Message> messageList = new ArrayList<>();
        //添加系统提示词
        addAndSave(messageList, systemMessage());
        //加载记忆
        messageList.addAll(memoryManager.loadMemory(query));
        //条件用户提示词
        addAndSave(messageList, new UserMessage(query));
        return messageList;
    }

    /**
     * 规划
     *
     * @param messageList
     * @return
     */
    @Override
    public ChatResponse plan(List<Message> messageList) {
        return chatModel.call(messageList, toolManager.getTools());
    }

    /**
     * 行动
     *
     * @param toolCalls
     * @return
     */
    @Override
    public List<Message> action(List<AssistantMessage.ToolCall> toolCalls) {
        List<Message> toolMessageList = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            ToolMessage toolMessage = toolManager.invoke(toolCall);
            //添加工具调用的tool消息
            toolMessageList.add(toolMessage);
        }
        return toolMessageList;
    }

    protected int getMaxLoopNum() {
        return MAX_LOOP_NUM;
    }
}
