package com.lyh.finance.controller;

import com.lyh.finance.context.RequestContext;
import com.lyh.finance.agent.ReActAgent;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.StreamEvent;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@RestController
@RequestMapping("/react")
public class ReActController {
    @Resource
    private ReActAgent reActAgent;

    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestParam("query") String query,
                                             @RequestHeader(value = "conversationId", required = false) String conversationId,
                                             @RequestHeader(value = "isNew", required = false) Boolean isNewHeader) {
        boolean isNew = Boolean.TRUE.equals(isNewHeader);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
            isNew = true;
        }
        RequestContext.setSession(conversationId, isNew);
        ChatResponse response = reActAgent.chat(query);
        RequestContext.clear();
        //将conversationId放入响应头
        return ResponseEntity.ok()
                .header("X-Session-Id", conversationId)
                .body(response);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> chatStream(@RequestParam("query") String query,
                                                 @RequestParam(value = "conversationId", required = false) String conversationId,
                                                 @RequestHeader(value = "isNew", required = false) Boolean isNewHeader) {
        boolean isNew = Boolean.TRUE.equals(isNewHeader);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
            isNew = true;
        }
        String finalConversationId = conversationId;
        boolean finalIsNew = isNew;
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                RequestContext.setSession(finalConversationId, finalIsNew);
                sendEvent(emitter, StreamEvent.session(finalConversationId));
                reActAgent.chatStream(query, event -> safeSend(emitter, event));
                sendEvent(emitter, StreamEvent.done());
                emitter.complete();
            } catch (Exception ex) {
                safeSend(emitter, StreamEvent.error(ex.getMessage()));
                emitter.completeWithError(ex);
            } finally {
                RequestContext.clear();
            }
        });
        return ResponseEntity.ok()
                .body(emitter);
    }

    private void safeSend(SseEmitter emitter, StreamEvent event) {
        try {
            sendEvent(emitter, event);
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void sendEvent(SseEmitter emitter, StreamEvent event) throws IOException {
        if (event == null) {
            return;
        }
        emitter.send(SseEmitter.event().name(event.getType()).data(event));
    }
}
