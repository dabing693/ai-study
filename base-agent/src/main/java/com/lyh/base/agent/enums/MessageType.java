package com.lyh.base.agent.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum MessageType {
    user,
    assistant,
    system,
    tool;
    private static final Map<String, MessageType> map = Arrays.stream(MessageType.values()).collect(Collectors.toMap(MessageType::name, it -> it));

    public MessageType of(String type) {
        return map.get(type);
    }
}