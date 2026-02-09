package com.lyh.base.agent.domain.message;

import com.lyh.base.agent.enums.MessageType;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class UserMessage extends Message {
    public UserMessage(String content) {
        super(MessageType.user.name(), content);
    }
}