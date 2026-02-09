package com.lyh.base.agent.memory.repository;

import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.memory.MemoryQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class RamMemoryRepository implements IMemoryRepository<Message, Message> {
    private static final Map<String, List<Message>> messageMap = new HashMap<>();

    @Override
    public List<Message> add(String conversationId, List<Message> messages) {
        messageMap.computeIfAbsent(conversationId, key -> new ArrayList<>()).addAll(messages);
        return messages;
    }

    @Override
    public List<Message> get(MemoryQuery query) {
        List<Message> list = messageMap.getOrDefault(query.getConversationId(), new ArrayList<>());
        if (list.size() > query.getLimit()) {
            return list.subList(0, query.getLimit());
        }
        return list;
    }
}
