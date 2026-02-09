package com.lyh.finance.memory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Milvus 向量迁移测试类
 * <p>
 * 功能：将 Milvus 中的向量从 Gemini embedding 模型迁移到 Ollama embedding 模型
 * 步骤：
 * 1. 从 Milvus 查询所有数据（id, content）
 * 2. 使用 Ollama 重新生成向量
 * 3. 删除旧数据并插入新向量（upsert 操作）
 *
 * @author lengYinHui
 * @date 2026/2/8
 */
@Slf4j
@SpringBootTest
public class MilvusEmbeddingMigrationTest {

    @Value("${milvus.collection-name:finance_agent_memory}")
    private String collectionName;

    @Autowired
    private MilvusClientV2 milvusClientV2;

    @Autowired
    private EmbeddingModel ollamaEmbeddingModel;

    private static final int BATCH_SIZE = 100;

    /**
     * 执行向量迁移
     * 从 Milvus 读取数据，使用 Ollama 重新生成向量，然后更新回 Milvus
     */
    @Test
    void migrateEmbeddingToOllama() {
        long offset = 0;
        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;

        log.info("开始向量迁移，目标集合: {}", collectionName);

        while (true) {
            // 1. 批量查询 Milvus 数据
            List<LlmMemoryVector> records = fetchRecordsFromMilvus(offset, BATCH_SIZE);
            if (CollectionUtils.isEmpty(records)) {
                log.info("没有更多数据，查询完成");
                break;
            }

            log.info("批次处理: offset={}, size={}", offset, records.size());
            totalProcessed += records.size();

            // 2. 使用 Ollama 生成新向量
            List<LlmMemoryVector> updatedRecords = new ArrayList<>();
            for (LlmMemoryVector record : records) {
                try {
                    String content = record.getContent();
                    if (!StringUtils.hasText(content)) {
                        log.warn("记录内容为空，跳过 id={}", record.getId());
                        totalFailed++;
                        continue;
                    }

                    // 使用 Ollama 生成向量
                    List<Float> newVector = ollamaEmbeddingModel.genVector(content);
                    if (CollectionUtils.isEmpty(newVector)) {
                        log.warn("生成向量失败，跳过 id={}", record.getId());
                        totalFailed++;
                        continue;
                    }

                    record.setContent_vector(newVector);
                    updatedRecords.add(record);
                    totalSuccess++;

                } catch (Exception e) {
                    log.error("生成向量异常，id={}", record.getId(), e);
                    totalFailed++;
                }
            }

            // 3. 批量更新到 Milvus（先删除后插入）
            if (!CollectionUtils.isEmpty(updatedRecords)) {
                try {
                    upsertBatch(updatedRecords);
                    log.info("批次更新成功，条数: {}", updatedRecords.size());
                } catch (Exception e) {
                    log.error("批次更新失败", e);
                    totalFailed += updatedRecords.size();
                    totalSuccess -= updatedRecords.size();
                }
            }

            offset += records.size();
        }

        log.info("向量迁移完成！总计处理: {}, 成功: {}, 失败: {}",
                totalProcessed, totalSuccess, totalFailed);
    }

    /**
     * 从 Milvus 查询记录
     */
    private List<LlmMemoryVector> fetchRecordsFromMilvus(long offset, int limit) {
        QueryReq queryReq = QueryReq.builder()
                .collectionName(collectionName)
                .filter("id >= 0")
                .outputFields(List.of("id", "content", "type", "conversation_id", "content_vector"))
                .offset(offset)
                .limit(limit)
                .build();

        QueryResp resp = milvusClientV2.query(queryReq);
        List<QueryResp.QueryResult> queryResults = resp.getQueryResults();

        if (CollectionUtils.isEmpty(queryResults)) {
            return List.of();
        }

        List<LlmMemoryVector> records = new ArrayList<>();
        for (QueryResp.QueryResult result : queryResults) {
            try {
                Map<String, Object> entity = result.getEntity();
                LlmMemoryVector vector = new LlmMemoryVector();
                vector.setId(Long.parseLong(String.valueOf(entity.get("id"))));
                vector.setContent(String.valueOf(entity.get("content")));
                vector.setType(String.valueOf(entity.get("type")));
                vector.setConversation_id(String.valueOf(entity.get("conversation_id")));
                @SuppressWarnings("unchecked")
                List<Float> contentVector = (List<Float>) entity.get("content_vector");
                vector.setContent_vector(contentVector);
                records.add(vector);
            } catch (Exception e) {
                log.warn("解析记录失败: {}", result, e);
            }
        }

        return records;
    }

    /**
     * 批量 upsert 操作
     * 使用 Milvus 的 Upsert API，底层原理：
     * 1. 通过 Bloom Filter 检查 PK 是否存在
     * 2. 存在的记录标记删除（Delete Bitmask）
     * 3. 追加写入新记录（LSM-Tree 追加写）
     * 4. 操作原子性：单批次内要么全成功，要么全失败
     */
    private void upsertBatch(List<LlmMemoryVector> list) {
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

        UpsertReq upsertReq = UpsertReq.builder()
                .collectionName(collectionName)
                .data(rows)
                .build();
        milvusClientV2.upsert(upsertReq);
    }
}
