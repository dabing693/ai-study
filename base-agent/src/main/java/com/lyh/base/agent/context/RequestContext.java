package com.lyh.base.agent.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.util.context.ContextView;

import java.util.Optional;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
public class RequestContext {

    /**
     * 使用 final 保证引用不可变，static 保证全局唯一
     */
    private static final TransmittableThreadLocal<UserContext> USER_CONTEXT =
            new TransmittableThreadLocal<>();
    public static final String USER_CONTEXT_KEY = "userContext";

    public static void setSession(String conversationId, Boolean newConversation) {
        USER_CONTEXT.set(UserContext.of(conversationId, newConversation));
    }

    public static void setUser(UserContext user) {
        USER_CONTEXT.set(user);
    }

    public static String getSession() {
        return Optional.ofNullable(USER_CONTEXT.get())
                .map(UserContext::getConversationId)
                .orElseThrow();
    }

    public static Boolean isNewConversation() {
        return Optional.ofNullable(USER_CONTEXT.get())
                .map(UserContext::getNewConversation)
                .orElseThrow();
    }

    public static UserContext getUser() {
        return USER_CONTEXT.get();
    }

    /**
     * 从 Reactor Context 中复制 userContext
     * 用于响应式编程环境
     */
    public static void copyUserContextFromReactive(ContextView context) {
        UserContext userContext = context.get(USER_CONTEXT_KEY);
        if (userContext != null) {
            RequestContext.setUser(userContext);
        } else {
            // 如果 Context 中不存在这些值，说明是非响应式调用，跳过
            // 此时应该已经通过其他方式设置了 RequestContext
            log.warn("非响应式调用...");
        }
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }

    @Data
    public static class UserContext {
        private String conversationId;
        private Boolean newConversation;

        public static UserContext of(String conversationId, Boolean newConversation) {
            final UserContext userContext = new UserContext();
            userContext.setConversationId(conversationId);
            userContext.setNewConversation(newConversation);
            return userContext;
        }
    }
}
