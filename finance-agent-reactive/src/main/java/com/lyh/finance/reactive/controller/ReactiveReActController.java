package com.lyh.finance.reactive.controller;

import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.define.ReactiveReActAgent;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.finance.service.ConversationService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@RestController
@RequestMapping("/react")
public class ReactiveReActController {
    @Resource
    private ReactiveReActAgent reactiveReActAgent;

    @Autowired
    private ConversationService conversationService;

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamEvent> chatStream(@RequestParam("query") String query,
                                        @RequestParam(value = "conversationId", required = false) String conversationId,
                                        @RequestHeader(value = "isNew", required = false) String isNewHeader,
                                        ServerWebExchange exchange) {
        boolean isNew = "true".equalsIgnoreCase(isNewHeader);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
            isNew = true;
        }
        // 获取用户ID，如果已登录则保存对话
        Long userId = exchange.getAttribute("userId");
        String finalConversationId = conversationId;
        boolean finalIsNew = isNew;

        // 如果是新对话且用户已登录，保存会话记录
        if (finalIsNew && userId != null) {
            String title = query.length() > 30 ? query.substring(0, 30) + "..." : query;
            conversationService.createOrUpdateConversation(finalConversationId, userId, title);
        }

        // 构造起始事件
        StreamEvent sessionEvent = StreamEvent.session(finalConversationId);

        // 使用响应式方法，通过 Reactor Context 传递会话信息
        return Flux.concat(
                Flux.just(sessionEvent),
                reactiveReActAgent.chatFluxReactive(query)
                        .contextWrite(Context.of(
                                RequestContext.USER_CONTEXT_KEY,
                                RequestContext.UserContext.of(conversationId, isNew)
                        ))
        ).concatWith(Flux.just(StreamEvent.done()));
    }

    @PostMapping("/chat/reactive")
    public Mono<ResponseEntity<ChatResponse>> chatReactive(@RequestParam("query") String query,
                                                           @RequestHeader(value = "conversationId", required = false) String conversationId,
                                                           @RequestHeader(value = "isNew", required = false) Boolean isNewHeader) {
        boolean isNew = Boolean.TRUE.equals(isNewHeader);
        String finalConversationId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : conversationId;

        return reactiveReActAgent.chatMono(query)
                .map(response -> ResponseEntity.ok()
                        .header("X-Session-Id", finalConversationId)
                        .body(response))
                .contextWrite(Context.of(
                        RequestContext.USER_CONTEXT_KEY,
                        RequestContext.UserContext.of(finalConversationId, isNew)
                ));
    }
}
