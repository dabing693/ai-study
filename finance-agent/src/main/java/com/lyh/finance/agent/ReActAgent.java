package com.lyh.finance.agent;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.ToolMessage;
import com.lyh.finance.domain.message.UserMessage;
import com.lyh.finance.model.ChatModel;
import com.lyh.finance.tool.ToolInvoker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReActAgent {
    private final ChatModel chatModel;

    public ChatResponse chat(String query) {
        ChatResponse response = chatModel.call(new UserMessage(query));
        while (response.hasToolCalls()) {
            List<Message> currentMessage = new ArrayList<>();
            //添加模型返回的assistant消息
            currentMessage.add(response.getMessage());
            List<AssistantMessage.ToolCall> toolCalls = response.getToolCalls();
            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                ToolMessage toolMessage = ToolInvoker.invoke(toolCall);
                //添加工具调用的tool消息
                currentMessage.add(toolMessage);
            }
            response = chatModel.call(currentMessage);
        }
        return response;
    }
}
