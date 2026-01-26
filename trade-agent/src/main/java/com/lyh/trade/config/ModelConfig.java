package com.lyh.trade.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
