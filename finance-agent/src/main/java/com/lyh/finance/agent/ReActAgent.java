package com.lyh.finance.agent;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.*;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.ChatModel;
import com.lyh.finance.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;
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

    public ReActAgent(ChatModel chatModel, MemoryManager memoryManager, ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }

    @Override
    public ChatResponse chat(String query) {
        List<Message> messageList = sense(query);
        ChatResponse planResponse = plan(messageList);
        while (planResponse.hasToolCalls()) {
            //添加模型返回的assistant消息
            messageList.add(planResponse.getMessage());
            //行动
            messageList.addAll(action(planResponse.getToolCalls()));
            planResponse = plan(messageList);
        }
        //添加模型返回的assistant消息
        messageList.add(planResponse.getMessage());
        //保存记忆
        memoryManager.saveMemory(messageList);
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
        messageList.add(systemMessage());
        //加载记忆
        messageList.addAll(memoryManager.loadMemory(query));
        //条件用户提示词
        messageList.add(new UserMessage(query));
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
