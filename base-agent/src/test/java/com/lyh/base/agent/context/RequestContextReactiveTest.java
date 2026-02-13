package com.lyh.base.agent.context;

import io.milvus.param.R;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RequestContext 在响应式环境中的使用示例
 */
public class RequestContextReactiveTest {

    @Test
    public void testContextFlow() {
        String conversationId = "test-conversation-123";
        Boolean isNew = false;

        // 模拟响应式流中的 Context 传递
        Mono<String> result = Mono.deferContextual(context -> {
                    // 从 Context 中获取数据
                    RequestContext.copyUserContextFromReactive(context);

                    // 验证数据正确性
                    assertEquals(conversationId, RequestContext.getSession());
                    assertEquals(isNew, RequestContext.isNewConversation());

                    // 设置到 ThreadLocal
                    RequestContext.setSession(RequestContext.getSession(), RequestContext.isNewConversation());

                    // 验证 ThreadLocal 中可以获取
                    assertEquals(conversationId, RequestContext.getSession());
                    assertEquals(isNew, RequestContext.isNewConversation());

                    return Mono.just("success");
                })
                .doFinally(signalType -> RequestContext.clear())
                .contextWrite(Context.of(
                        RequestContext.USER_CONTEXT_KEY, RequestContext.UserContext.of(conversationId, isNew)
                ));

        String response = result.block();
        assertEquals("success", response);
    }

    @Test
    public void testFluxWithContext() {
        String conversationId = "test-conversation-456";
        Boolean isNew = true;

        Flux<String> result = Flux.<String>deferContextual(context -> {
                    RequestContext.copyUserContextFromReactive(context);
                    return Flux.just(
                            RequestContext.getSession(),
                            String.valueOf(RequestContext.isNewConversation())
                    );
                })
                .doFinally(signalType -> RequestContext.clear())
                .contextWrite(Context.of(
                        RequestContext.USER_CONTEXT_KEY, RequestContext.UserContext.of(conversationId, isNew)
                ));

        result.subscribe(
                item -> {
                    if (item.equals(conversationId)) {
                        assertEquals(conversationId, item);
                    } else {
                        assertEquals(String.valueOf(isNew), item);
                    }
                }
        ).dispose();
    }

    @Test
    public void testContextMissing() {
        // 没有 Context 时应该抛异常
        Mono<String> result = Mono.deferContextual(context -> {
                    assertThrows(IllegalStateException.class,
                            () -> RequestContext.getSession());
                    return Mono.just("error");
                })
                .contextWrite(Context.empty());

        result.block();
    }
}
