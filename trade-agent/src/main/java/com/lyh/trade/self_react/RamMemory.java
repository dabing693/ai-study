package com.lyh.trade.self_react;

import com.lyh.trade.self_react.domain.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class RamMemory {
    private static final Map<String, List<Message>> messageMap = new HashMap<>();

    public static void add(String uniqueId, Message message) {
        messageMap.computeIfAbsent(uniqueId, key -> new ArrayList<>()).add(message);
    }

    public static List<Message> get(String uniqueId) {
        return messageMap.getOrDefault(uniqueId, new ArrayList<>());
    }
}
