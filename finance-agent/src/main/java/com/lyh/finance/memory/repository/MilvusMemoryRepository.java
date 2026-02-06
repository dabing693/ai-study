package com.lyh.finance.memory.repository;

import com.lyh.finance.domain.DO.LlmMemory;
import com.lyh.finance.domain.DO.LlmMemoryVector;
import com.lyh.finance.embedding.GeminiEmbed;
import com.lyh.finance.embedding.VectorSearchService;
import com.lyh.finance.enums.MessageType;
import com.lyh.finance.memory.MemoryQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusMemoryRepository implements IMemoryRepository<LlmMemory, Long> {
    private final GeminiEmbed geminiEmbed;
    private final VectorSearchService vectorSearchService;

    @Override
    public List<Long> add(String conversationId, List<LlmMemory> messages) {
        List<LlmMemoryVector> vectorList = new ArrayList<>();
        for (LlmMemory it : messages) {
            //system tool消息不保存，因为向量搜索的目的是召回相关的历史记忆。
            // 示例1：它今天表现咋样？示例2：你之前提到的xxx，是啥意思？
            // 示例1：得过一层query改写解决
            if (Objects.equals(it.getType(), MessageType.system) ||
                    Objects.equals(it.getType(), MessageType.tool)) {
                continue;
            }
            LlmMemoryVector vector = new LlmMemoryVector();
            vector.setId(it.getId());
            vector.setType(it.getType().name());
            vector.setConversation_id(it.getConversationId());
            vector.setContent_vector(geminiEmbed.genVector(it.getContent()));
            vectorList.add(vector);
        }
        if (CollectionUtils.isEmpty(vectorList)) {
            return Collections.emptyList();
        }
        vectorSearchService.insertBatch(vectorList);
        log.info("成功保存到向量数据库，条数：{}", vectorList.size());
        return vectorList.stream().map(LlmMemoryVector::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> get(MemoryQuery query) {
        return vectorSearchService.searchSimilarVectors(query.getConversationId(),
                query.getQuery(), query.getLimit());
    }
}
