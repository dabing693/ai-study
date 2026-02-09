package com.lyh.base.agent.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;

import java.util.Optional;
import java.util.UUID;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class RequestContext {
    private static final String TEST_PREFIX = "test_";
    /**
     * 使用 final 保证引用不可变，static 保证全局唯一
     */
    private static final TransmittableThreadLocal<UserContext> USER_CONTEXT =
            new TransmittableThreadLocal<>();

    public static void setSession(String conversationId, Boolean newConversation) {
        USER_CONTEXT.set(UserContext.of(conversationId, newConversation));
    }

    public static void setUser(UserContext user) {
        USER_CONTEXT.set(user);
    }

    public static String getSession() {
        return Optional.ofNullable(USER_CONTEXT.get())
                .map(UserContext::getConversationId)
                .orElse(TEST_PREFIX + System.currentTimeMillis());
    }

    public static Boolean isNewConversation() {
        return Optional.ofNullable(USER_CONTEXT.get())
                .map(UserContext::getNewConversation)
                .orElse(true);
    }

    public static UserContext getUser() {
        return USER_CONTEXT.get();
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
