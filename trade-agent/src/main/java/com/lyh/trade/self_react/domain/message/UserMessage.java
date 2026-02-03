package com.lyh.trade.self_react.domain.message;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class UserMessage extends Message {
    public UserMessage(String content) {
        super("user", content);
    }
}