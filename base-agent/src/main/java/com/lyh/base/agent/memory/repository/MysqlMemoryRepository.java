package com.lyh.base.agent.memory.repository;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.AgentTurn;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.base.agent.mapper.LlmTurnMapper;
import com.lyh.base.agent.memory.MemoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
@RequiredArgsConstructor
public class MysqlMemoryRepository implements IMemoryRepository<Message, LlmMemory> {
    private final LlmMemoryMapper llmMemoryMapper;
    private final LlmTurnMapper llmTurnMapper;

    @Override
    public List<LlmMemory> add(String conversationId, List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }
        RequestContext.UserContext userContext = RequestContext.getUser();
        Long turnId = (userContext != null) ? userContext.getTurnId() : null;
        if (turnId == null) {
            AgentTurn turn = new AgentTurn();
            turn.setConversationId(conversationId);
            turn.setCreateTime(LocalDateTime.now());
            llmTurnMapper.insert(turn);
            turnId = turn.getId();
            if (userContext != null) {
                userContext.setTurnId(turnId);
            }
        }
        
        List<LlmMemory> llmMemories = message2Memory(conversationId, turnId, messages);
        for (LlmMemory mem : llmMemories) {
            llmMemoryMapper.insert(mem);
        }
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

    public List<LlmMemory> selectByIds(List<Long> ids){
       return llmMemoryMapper.selectByIds(ids);
    }

    public long countTurns(String conversationId, Long minTurnId) {
        QueryWrapper<AgentTurn> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minTurnId != null) {
            queryWrapper.gt("id", minTurnId);
        }
        return llmTurnMapper.selectCount(queryWrapper);
    }

    /**
     * 获取倒序第 offset 个 turn 的 ID（常用来计算 activeWindow 界限）
     */
    public Long getTurnBoundaryIdDesc(String conversationId, Long minTurnId, int offset) {
        QueryWrapper<AgentTurn> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minTurnId != null) {
            queryWrapper.gt("id", minTurnId);
        }
        queryWrapper.orderByDesc("id").last("limit " + offset + ", 1");
        AgentTurn turn = llmTurnMapper.selectOne(queryWrapper);
        return turn != null ? turn.getId() : null;
    }

    /**
     * 获取正序第 offset 个 turn 的 ID（用来计算最老需要压缩的一批界限）
     */
    public Long getTurnBoundaryIdAsc(String conversationId, Long minTurnId, int offset) {
        QueryWrapper<AgentTurn> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minTurnId != null) {
            queryWrapper.gt("id", minTurnId);
        }
        queryWrapper.orderByAsc("id").last("limit " + offset + ", 1");
        AgentTurn turn = llmTurnMapper.selectOne(queryWrapper);
        return turn != null ? turn.getId() : null;
    }

    /**
     * 通过 turn_id 的范围来提取全量消息
     */
    public List<LlmMemory> getMessagesByTurnRange(String conversationId, Long minTurnId, Long maxTurnId) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        if (minTurnId != null) {
            queryWrapper.gt("turn_id", minTurnId);
        }
        if (maxTurnId != null) {
            queryWrapper.lt("turn_id", maxTurnId);
        }
        queryWrapper.orderByAsc("id");
        return llmMemoryMapper.selectList(queryWrapper);
    }

    /**
     * 取最近的 limit 轮的所有消息
     */
    public List<LlmMemory> getRecentTurnMessages(String conversationId, Long minTurnId, int limit) {
        // 先获取倒数第 limit 个 turnId
        QueryWrapper<AgentTurn> turnGw = new QueryWrapper<>();
        turnGw.eq("conversation_id", conversationId);
        if (minTurnId != null) {
            turnGw.gt("id", minTurnId);
        }
        turnGw.orderByDesc("id").last("limit " + limit);
        List<AgentTurn> recentTurns = llmTurnMapper.selectList(turnGw);
        if (CollectionUtils.isEmpty(recentTurns)) {
            return Collections.emptyList();
        }
        List<Long> turnIds = recentTurns.stream().map(AgentTurn::getId).collect(Collectors.toList());
        
        QueryWrapper<LlmMemory> memGw = new QueryWrapper<>();
        memGw.eq("conversation_id", conversationId);
        memGw.in("turn_id", turnIds);
        memGw.orderByDesc("id");
        return llmMemoryMapper.selectList(memGw);
    }

    private List<LlmMemory> message2Memory(String conversationId, Long turnId, List<Message> messages) {
        List<LlmMemory> list = new ArrayList<>();
        for (Message message : messages) {
            LlmMemory llmMemory = new LlmMemory();
            llmMemory.setConversationId(conversationId);
            llmMemory.setContent(message.storedContent());
            llmMemory.setType(MessageType.valueOf(message.getRole()));
            llmMemory.setTimestamp(message.getCreate());
            llmMemory.setJsonContent(JSONObject.toJSONString(message));
            llmMemory.setTurnId(turnId);
            list.add(llmMemory);
        }
        return list;
    }
}
