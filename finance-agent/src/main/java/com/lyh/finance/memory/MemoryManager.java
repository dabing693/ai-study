package com.lyh.finance.memory;

import com.lyh.finance.context.RequestContext;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.enums.MemoryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MemoryManager {
    private final MemoryProperty memoryProperty;
    private final MysqlMemory mysqlMemory;

    public List<Message> relatedHistoryMemory(String query) {
        List<Message> hisMessages = mysqlMemory.get(RequestContext.getSession(),
                memoryProperty.getMaxMessageNum(),
                query, MemoryStrategy.valueOf(memoryProperty.getStrategy()));
        return hisMessages;
    }

    public int save(List<Message> messages) {
        mysqlMemory.addAll(messages);
        return messages.size();
    }

    public Integer getMaxMessageNum() {
        return memoryProperty.getMaxMessageNum();
    }
}
