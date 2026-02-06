package com.lyh.finance.agent;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.*;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
public class ReActAgent extends BaseAgent {
    private static final String system_prompt = """
            你是一个乐于助人的金融领域专家，你善于利用各种工具解决用户的问题。
            当前时间：{cur_date_time}
            """;
    @Value("${max.loop.num:10}")
    private Integer maxLoopNum;

    public ReActAgent(ChatModel chatModel, MemoryManager memoryManager, ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }

    @Override
    public ChatResponse chat(String query) {
        List<Message> messageList = sense(query);
        ChatResponse planResponse = null;
        for (int i = 0; i < maxLoopNum; i++) {
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

    @Override
    public SystemMessage systemMessage() {
        return new SystemMessage(system_prompt.replace("{cur_date_time}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(new Date())));
    }
}
