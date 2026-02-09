package com.lyh.base.agent.memory.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.MemoryQuery;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.WeightedRanker;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusMemoryRepository implements IMemoryRepository<LlmMemory, LlmMemoryVector> {
    //todo 按agent区分 agent名称从上游传入
    @Value("${milvus.collection-name:finance_agent_memory}")
    private String collectionName;
    private final MilvusClientV2 milvusClientV2;
    private final EmbeddingModel embeddingModel;

    @Override
    public List<LlmMemoryVector> add(String conversationId, List<LlmMemory> messages) {
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
            vector.setContent(it.getContent());
            vector.setContent_vector(embeddingModel.genVector(it.getContent()));
            vectorList.add(vector);
        }
        if (CollectionUtils.isEmpty(vectorList)) {
            return Collections.emptyList();
        }
        insertBatch(vectorList);
        log.info("成功保存到向量数据库，条数：{}", vectorList.size());
        return vectorList.stream()
                .map(vector -> {
                    LlmMemoryVector summary = new LlmMemoryVector();
                    summary.setId(vector.getId());
                    summary.setContent(vector.getContent());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<LlmMemoryVector> get(MemoryQuery query) {
        if (!StringUtils.hasText(query.getQuery())) {
            return Collections.emptyList();
        }
        return hybridSearch(query);
    }

    private List<LlmMemoryVector> hybridSearch(MemoryQuery query) {
        // 1) 构造会话范围过滤条件，避免跨会话召回。
        String filter = buildConversationFilter(query.getConversationId());
        // 2) 确定召回条数上限。
        int limit = query.getLimit() == null ? 10 : query.getLimit();
        // 3) 稀疏向量检索：使用全文文本触发BM25。
        AnnSearchReq sparseRequest = AnnSearchReq.builder()
                .vectorFieldName("content_embeddings")
                .metricType(IndexParam.MetricType.BM25)
                .vectors(Collections.singletonList(new EmbeddedText(query.getQuery())))
                .filter(filter)
                .limit(limit)
                .build();
        // 4) 生成密集向量，用于向量相似度检索。
        List<Float> queryVector = embeddingModel.genVector(query.getQuery());
        // 5) 密集向量检索：使用COSINE度量。
        AnnSearchReq denseRequest = AnnSearchReq.builder()
                .vectorFieldName("content_vector")
                .metricType(IndexParam.MetricType.COSINE)
                .vectors(Collections.singletonList(new FloatVec(queryVector)))
                .filter(filter)
                .limit(limit)
                .build();
        // 6) 组合两路检索，并用RRF融合排序。
        HybridSearchReq hybridSearchReq = HybridSearchReq.builder()
                .collectionName(collectionName)
                .searchRequests(Arrays.asList(sparseRequest, denseRequest))
                .ranker(new WeightedRanker(Arrays.asList(0.6f, 0.4f)))
                .limit(limit)
                .outFields(Arrays.asList("id", "content"))
                .build();
        // 7) 执行混合检索。
        SearchResp resp = milvusClientV2.hybridSearch(hybridSearchReq);
        // 8) 解析结果并抽取主键ID。
        List<List<SearchResp.SearchResult>> searchResults = resp.getSearchResults();
        if (CollectionUtils.isEmpty(searchResults) || CollectionUtils.isEmpty(searchResults.get(0))) {
            return Collections.emptyList();
        }
        Double minScore = query.getMinScore();
        return searchResults.get(0).stream()
                .filter(result -> minScore == null || result.getScore() == null || result.getScore() >= minScore)
                .map(result -> {
                    LlmMemoryVector vector = new LlmMemoryVector();
                    vector.setId(parseId(result.getId()));
                    Map<String, Object> fields = result.getEntity();
                    if (fields != null) {
                        Object content = fields.get("content");
                        if (content != null) {
                            vector.setContent(String.valueOf(content));
                        }
                    }
                    return vector;
                })
                .filter(vector -> vector.getId() != null && StringUtils.hasText(vector.getContent()))
                .collect(Collectors.toList());
    }

    private String buildConversationFilter(String conversationId) {
        String escapedConversationId = conversationId.replace("'", "\\'");
        return String.format("conversation_id == '%s'", escapedConversationId);
    }

    private Long parseId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量插入
     *
     * @param list
     * @return
     * @throws IllegalAccessException
     */
    private int insertBatch(List<LlmMemoryVector> list) {
        List<JsonObject> rows = new ArrayList<>(list.size());
        for (LlmMemoryVector vector : list) {
            JsonObject row = new JsonObject();
            row.addProperty("id", vector.getId());
            row.addProperty("type", vector.getType());
            row.addProperty("conversation_id", vector.getConversation_id());
            row.addProperty("content", vector.getContent());
            JsonArray contentVector = new JsonArray();
            for (Float value : vector.getContent_vector()) {
                contentVector.add(value);
            }
            row.add("content_vector", contentVector);
            rows.add(row);
        }
        InsertReq insertReq = InsertReq.builder()
                .collectionName(collectionName)
                .data(rows)
                .build();
        milvusClientV2.insert(insertReq);
        return rows.size();
    }
}
