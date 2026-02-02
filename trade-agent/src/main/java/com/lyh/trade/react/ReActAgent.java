package com.lyh.trade.react;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Slf4j
@Component
public class ReActAgent {
    @Resource(name = "noToolCallAdvisorChatClient")
    private ChatClient chatClient;
    @Resource
    private ToolCallbackProvider toolCallbackProvider;
    private final DefaultToolCallingManager toolCallingManager = ToolCallingManager.builder().build();


    public ChatResponse chat(String query, String sessionId) {
        Prompt prompt = new Prompt(query);
        ChatResponse response = chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .chatResponse();
        if (!response.hasToolCalls()) {
            return response;
        }
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        while (response.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = response.getResult().getOutput().getToolCalls();

            for (AssistantMessage.ToolCall toolCall : toolCalls) {
                String name = toolCall.name();
                String arguments = toolCall.arguments();
                String id = toolCall.id();
                log.info("开始执行工具调用，name：{}，arguments：{}", name, arguments);
                ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response);
                Prompt newPrompt = new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions());
                log.info("工具调用成功，name：{}，arguments：{}", name, arguments);
            }
        }
        return null;
    }
}
