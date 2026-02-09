package com.lyh.base.agent.domain.message;

import com.lyh.base.agent.enums.MessageType;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class SystemMessage extends Message {
    public SystemMessage(String content) {
        super(MessageType.system.name(), content);
    }
}
