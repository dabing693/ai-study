package com.lyh.trade.self_react.domain.message;

import lombok.Data;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class Message {
    private String role;
    private String content;

    public Message() {

    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
}