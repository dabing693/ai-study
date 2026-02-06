package com.lyh.finance.memory.repository;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.finance.domain.DO.LlmMemory;
import com.lyh.finance.domain.message.*;
import com.lyh.finance.enums.MessageType;
import com.lyh.finance.mapper.LlmMemoryMapper;
import com.lyh.finance.memory.MemoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlMemoryRepository implements IMemoryRepository<Message, LlmMemory> {
    private final LlmMemoryMapper llmMemoryMapper;

    @Override
    public List<LlmMemory> add(String conversationId, List<Message> messages) {
        List<LlmMemory> llmMemories = message2Memory(conversationId, messages);
        llmMemoryMapper.insert(llmMemories);
        return llmMemories;
    }

    @Override
    public List<LlmMemory> get(MemoryQuery query) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", query.getConversationId());
        //不查系统提示词
        queryWrapper.ne("type", MessageType.system.name());
        //按时间降序排序 优先取最新的
        queryWrapper.orderByDesc("timestamp");
        queryWrapper.last("limit " + query.getLimit());
        return llmMemoryMapper.selectList(queryWrapper);
    }

    public List<LlmMemory> getByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);

        return llmMemoryMapper.selectList(queryWrapper);
    }

    private List<LlmMemory> message2Memory(String conversationId, List<Message> messages) {
        List<LlmMemory> list = new ArrayList<>();
        for (Message message : messages) {
            LlmMemory llmMemory = new LlmMemory();
            llmMemory.setConversationId(conversationId);
            llmMemory.setContent(message.storedContent());
            llmMemory.setType(MessageType.valueOf(message.getRole()));
            llmMemory.setTimestamp(message.getCreate());
            llmMemory.setJsonContent(JSONObject.toJSONString(message));
            list.add(llmMemory);
        }
        return list;
    }
}
