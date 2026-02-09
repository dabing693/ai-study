package com.lyh.finance.agent;

import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.StreamChatResult;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.ToolMessage;
import com.lyh.base.agent.domain.message.UserMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author claude code with kimi
 * @date 2026/2/6
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
                List<Message> toolMessages = actionWithCall(planResponse.getToolCalls());
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
                    // 保存 tool call 消息
                    String callContent = "Call " + (toolCall.getFunction() != null ? toolCall.getFunction().getName() : "Tool")
                            + "\n" + (toolCall.getFunction() != null ? toolCall.getFunction().getArguments() : "");
                    ToolMessage callMessage = new ToolMessage(callContent, toolCall.getId());
                    addAndSave(messageList, callMessage);
                    // 调用工具
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

    /**
     * 行动（保存 tool call 和 tool result）
     *
     * @param toolCalls
     * @return
     */
    public List<Message> actionWithCall(List<AssistantMessage.ToolCall> toolCalls) {
        List<Message> toolMessageList = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            // 保存 tool call
            String callContent = "Call " + (toolCall.getFunction() != null ? toolCall.getFunction().getName() : "Tool")
                    + "\n" + (toolCall.getFunction() != null ? toolCall.getFunction().getArguments() : "");
            ToolMessage callMessage = new ToolMessage(callContent, toolCall.getId());
            toolMessageList.add(callMessage);
            // 调用工具并保存结果
            ToolMessage toolMessage = toolManager.invoke(toolCall);
            if (toolMessage != null) {
                toolMessageList.add(toolMessage);
            }
        }
        return toolMessageList;
    }

    protected int getMaxLoopNum() {
        return MAX_LOOP_NUM;
    }
}
