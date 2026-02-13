package com.lyh.finance.controller;

import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.finance.interceptor.AuthInterceptor;
import com.lyh.finance.service.ConversationService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@ConditionalOnProperty(prefix = "react.chat.stream.use", name = "reactive", havingValue = "false")
@RestController
@RequestMapping("/react")
public class ReActController {
    @Resource
    private ReActAgent reActAgent;

    @Autowired
    private ConversationService conversationService;

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
                                                 @RequestHeader(value = "isNew", required = false) String isNewHeader,
                                                 HttpServletResponse response) {
        //显式设置响应头
        response.setCharacterEncoding("UTF-8");
        boolean isNew = "true".equalsIgnoreCase(isNewHeader);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
            isNew = true;
        }

        // 获取用户ID，如果已登录则保存对话
        Long userId = AuthInterceptor.getCurrentUserId();
        String finalConversationId = conversationId;
        boolean finalIsNew = isNew;

        // 如果是新对话且用户已登录，保存会话记录
        if (finalIsNew && userId != null) {
            String title = query.length() > 30 ? query.substring(0, 30) + "..." : query;
            conversationService.createOrUpdateConversation(finalConversationId, userId, title);
        }

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
