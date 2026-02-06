package com.lyh.finance.memory.repository;

import com.lyh.finance.memory.MemoryQuery;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public interface IMemoryRepository<INPUT, OUTPUT> {
    default List<OUTPUT> add(String conversationId, INPUT message) {
        return add(conversationId, List.of(message));
    }

    List<OUTPUT> add(String conversationId, List<INPUT> messages);

    List<OUTPUT> get(MemoryQuery query);
}
