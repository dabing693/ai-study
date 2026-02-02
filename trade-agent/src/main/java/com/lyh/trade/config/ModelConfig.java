package com.lyh.trade.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@Configuration
public class ModelConfig {
    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(new SimpleLoggerAdvisor(), ToolCallAdvisor.builder().build())
                .build();
    }

    @Bean
    public ChatClient noLoggerChatClient(ChatModel chatModel,
                                         ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(ToolCallAdvisor.builder().build())
                .build();
    }

    @Bean
    public ChatClient noToolCallAdvisorChatClient(ChatModel chatModel,
                                                  ToolCallbackProvider toolCallbackProvider) {
        ZhiPuAiChatOptions chatOptions = new ZhiPuAiChatOptions.Builder()
                //内部是否执行工具调用 默认是null，也会执行工具调用 ToolCallingChatOptions::isInternalToolExecutionEnabled
                .internalToolExecutionEnabled(false)
                .build();
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultOptions(chatOptions)
                .build();
    }
}
