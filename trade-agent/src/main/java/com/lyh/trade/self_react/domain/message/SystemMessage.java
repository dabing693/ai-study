package com.lyh.trade.self_react.domain.message;

import com.lyh.trade.self_react.enums.MessageType;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class SystemMessage extends Message{
    public SystemMessage(String content) {
        super(MessageType.system.name(), content);
    }
}
