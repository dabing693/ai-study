package com.lyh.trade.self_react;

import com.lyh.trade.self_react.domain.dao.LlmMemory;
import com.lyh.trade.self_react.domain.message.Message;
import com.lyh.trade.self_react.enums.MessageType;
import com.lyh.trade.self_react.mapper.LlmMemoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Component
public class MysqlMemory {
    @Resource
    private LlmMemoryMapper llmMemoryMapper;

    public void add(Message message) {
        addAll(Collections.singletonList(message));
    }

    public void addAll(List<Message> messages) {
        List<LlmMemory> list = new ArrayList<>();
        for (Message message : messages) {
            LlmMemory llmMemory = new LlmMemory();
            llmMemory.setConversationId(RequestContext.getSession());
            llmMemory.setContent(message.storedContent());
            llmMemory.setType(MessageType.valueOf(message.getRole()));
            llmMemory.setTimestamp(message.getCreate());
            list.add(llmMemory);
        }
        llmMemoryMapper.insert(list);
    }

    public List<Message> get(String uniqueId) {
        return null;
    }
}
