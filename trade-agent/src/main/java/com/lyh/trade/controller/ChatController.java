package com.lyh.trade.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private static final String CONVERSATION_ID = "test";

    private final ChatClient chatClient;
    private final ChatClient noToolChatClient;

    public ChatController(ZhiPuAiChatModel zhiPuAiChatModel,
                          ToolCallbackProvider toolCallbackProvider,
                          ChatMemory chatMemory) {
        this.chatClient = ChatClient.builder(zhiPuAiChatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(new SimpleLoggerAdvisor(), ToolCallAdvisor.builder().build(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.noToolChatClient = ChatClient.builder(zhiPuAiChatModel)
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


    @GetMapping("/generate")
    public ChatResponse generate(@RequestParam("query") String query, @RequestHeader("sessionId") String sessionId) {
        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, StringUtils.hasLength(sessionId) ? sessionId : CONVERSATION_ID))
                .call()
                .chatResponse();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(@RequestParam(value = "query") String query, @RequestHeader("sessionId") String sessionId) {
        return noToolChatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, StringUtils.hasLength(sessionId) ? sessionId : CONVERSATION_ID))
                .stream()
                .chatResponse();
    }
}