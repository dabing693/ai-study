package com.lyh.trade.controller;

import com.lyh.trade.react.ReActAgent;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource(name = "chatClient")
    private ChatClient chatClient;
    @Resource(name = "noToolCallAdvisorChatClient")
    private ChatClient noToolCallAdvisorChatClient;
    @Resource
    private ReActAgent reActAgent;

    @GetMapping("/react")
    public ResponseEntity<ChatResponse> react(@RequestParam("query") String query,
                                              @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalSessionId = sessionId;
        ChatResponse chatResponse = reActAgent.chat(query, sessionId);
        // 将 sessionId 放入响应头
        return ResponseEntity.ok()
                .header("X-Session-Id", finalSessionId)
                .body(chatResponse);
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
        return noToolCallAdvisorChatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalSessionId))
                .stream()
                .chatResponse();
    }
}