package com.lyh.base.agent.memory.repository;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.*;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.base.agent.memory.MemoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
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
        if (query.getMinId() != null) {
            queryWrapper.gt("id", query.getMinId());
        }
        //不查系统提示词
        queryWrapper.ne("type", MessageType.system.name());
        //按时间降序排序、时间相同按id降序 优先取最新的
        queryWrapper.orderByDesc(List.of("timestamp", "id"));
        queryWrapper.last("limit " + query.getLimit());
        return llmMemoryMapper.selectList(queryWrapper);
    }

    public long countNormalMessages(String conversationId, Long minId) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minId != null) {
            queryWrapper.gt("id", minId);
        }
        // 排除系统/摘要消息
        queryWrapper.ne("type", MessageType.system.name());
        return llmMemoryMapper.selectCount(queryWrapper);
    }

    /**
     * 获取最早的 N 条普通消息（从位点之后开始）
     */
    public List<LlmMemory> getOldestMessages(String conversationId, Long minId, int limit) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minId != null) {
            queryWrapper.gt("id", minId);
        }
        queryWrapper.ne("type", MessageType.system.name())
                .orderByAsc("timestamp") // 升序，取最早的
                .last("limit " + limit);
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
