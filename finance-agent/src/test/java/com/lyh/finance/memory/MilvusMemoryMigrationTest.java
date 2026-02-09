package com.lyh.finance.memory;

import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.enums.MessageType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
public class MilvusMemoryMigrationTest {
    private static final String SOURCE_COLLECTION = "llm_memory_vectors";
    private static final String TARGET_COLLECTION = "finance_agent_memory";
    private static final int BATCH_SIZE = 200;

    @Autowired
    private LlmMemoryMapper llmMemoryMapper;
    @Autowired
    private MilvusClientV2 milvusClientV2;

    @Test
    void migrateMilvusToNewCollection() {
        long offset = 0;
        while (true) {
            Map<Long, List<Float>> vectorsById = fetchVectorsFromSource(offset, BATCH_SIZE);
            if (vectorsById.isEmpty()) {
                break;
            }
            List<Long> ids = new ArrayList<>(vectorsById.keySet());
            offset += ids.size();

            List<LlmMemory> memories = llmMemoryMapper.selectBatchIds(ids);
            if (CollectionUtils.isEmpty(memories)) {
                continue;
            }
            Map<Long, LlmMemory> memoryById = new HashMap<>();
            for (LlmMemory memory : memories) {
                memoryById.put(memory.getId(), memory);
            }

            List<LlmMemoryVector> vectorList = new ArrayList<>();
            for (Long id : ids) {
                LlmMemory it = memoryById.get(id);
                if (it == null) {
                    continue;
                }
                if (it.getType() == null ||
                        Objects.equals(it.getType(), MessageType.system) ||
                        Objects.equals(it.getType(), MessageType.tool)) {
                    continue;
                }
                if (!StringUtils.hasText(it.getContent())) {
                    continue;
                }
                List<Float> contentVector = vectorsById.get(id);
                if (CollectionUtils.isEmpty(contentVector)) {
                    continue;
                }
                LlmMemoryVector vector = new LlmMemoryVector();
                vector.setId(it.getId());
                vector.setType(it.getType().name());
                vector.setConversation_id(it.getConversationId());
                vector.setContent(it.getContent());
                vector.setContent_vector(contentVector);
                vectorList.add(vector);
            }
            if (!CollectionUtils.isEmpty(vectorList)) {
                insertBatch(vectorList);
                log.info("已迁移到Milvus，条数：{}", vectorList.size());
            }
        }
        log.info("迁移完成");
    }

    @Test
    void findMissingIds() {
        Set<Long> sourceIds = fetchIdsFromCollection(SOURCE_COLLECTION);
        Set<Long> targetIds = fetchIdsFromCollection(TARGET_COLLECTION);
        Set<Long> missing = new HashSet<>(sourceIds);
        missing.removeAll(targetIds);
        log.info("老集合总数：{}", sourceIds.size());
        log.info("新集合总数：{}", targetIds.size());
        if (missing.isEmpty()) {
            log.info("未发现缺失id");
            return;
        }
        List<Long> missingList = missing.stream().sorted().collect(Collectors.toList());
        log.warn("缺失id数量：{}", missingList.size());
        log.warn("缺失id列表：{}", missingList);
    }

    private Map<Long, List<Float>> fetchVectorsFromSource(long offset, int limit) {
        QueryReq queryReq = QueryReq.builder()
                .collectionName(SOURCE_COLLECTION)
                .filter("id >= 0")
                .outputFields(List.of("id", "content_vector"))
                .offset(offset)
                .limit(limit)
                .build();
        QueryResp resp = milvusClientV2.query(queryReq);
        List<QueryResp.QueryResult> queryResults = resp.getQueryResults();
        if (CollectionUtils.isEmpty(queryResults)) {
            return Map.of();
        }
        Map<Long, List<Float>> result = new HashMap<>();
        for (QueryResp.QueryResult record : queryResults) {
            try {
                Map<String, Object> entity = record.getEntity();
                Object idValue = entity.get("id");
                Object vectorValue = entity.get("content_vector");
                if (idValue == null || vectorValue == null) {
                    continue;
                }
                Long id = Long.parseLong(String.valueOf(idValue));
                @SuppressWarnings("unchecked")
                List<Float> vector = (List<Float>) vectorValue;
                if (!CollectionUtils.isEmpty(vector)) {
                    result.put(id, vector);
                }
            } catch (Exception e) {
                log.warn("读取content_vector失败，record: {}", record, e);
            }
        }
        return result;
    }

    private Set<Long> fetchIdsFromCollection(String collectionName) {
        Set<Long> ids = new HashSet<>();
        long offset = 0;
        while (true) {
            QueryReq queryReq = QueryReq.builder()
                    .collectionName(collectionName)
                    .filter("id >= 0")
                    .outputFields(List.of("id"))
                    .offset(offset)
                    .limit(BATCH_SIZE)
                    .build();
            QueryResp resp = milvusClientV2.query(queryReq);
            List<QueryResp.QueryResult> queryResults = resp.getQueryResults();
            if (CollectionUtils.isEmpty(queryResults)) {
                break;
            }
            for (QueryResp.QueryResult record : queryResults) {
                try {
                    Map<String, Object> entity = record.getEntity();
                    Object idValue = entity.get("id");
                    if (idValue == null) {
                        continue;
                    }
                    ids.add(Long.parseLong(String.valueOf(idValue)));
                } catch (Exception e) {
                    log.warn("读取id失败，collection: {}", collectionName, e);
                }
            }
            offset += queryResults.size();
        }
        return ids;
    }

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
                .collectionName(TARGET_COLLECTION)
                .data(rows)
                .build();
        milvusClientV2.insert(insertReq);
        return rows.size();
    }
}
