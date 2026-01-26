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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {
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
    public ResponseEntity<ChatResponse> generate(@RequestParam("query") String query,
                                                 @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalSessionId = sessionId;
        ChatResponse chatResponse = chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .call()
                .chatResponse();
        // 将 sessionId 放入响应头
        return ResponseEntity.ok()
                .header("X-Session-Id", finalSessionId)
                .body(chatResponse);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> generateStream(@RequestParam(value = "query") String query,
                                             @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalSessionId = sessionId;
        return noToolChatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .stream()
                .chatResponse();
    }
}