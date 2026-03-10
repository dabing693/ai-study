package com.lyh.base.agent.memory.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.base.agent.domain.DO.LlmSummary;
import com.lyh.base.agent.mapper.LlmSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SummaryRepository {
    private final LlmSummaryMapper llmSummaryMapper;

    /**
     * 获取最新的对话摘要
     */
    public LlmSummary getLatest(String conversationId) {
        QueryWrapper<LlmSummary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId)
                .orderByDesc("timestamp")
                .last("limit 1");
        return llmSummaryMapper.selectOne(queryWrapper);
    }

    /**
     * 保存单条摘要
     */
    public void save(LlmSummary summary) {
        llmSummaryMapper.insert(summary);
    }
}
