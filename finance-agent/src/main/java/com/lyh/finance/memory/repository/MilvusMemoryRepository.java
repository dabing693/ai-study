package com.lyh.finance.memory.repository;

import com.lyh.finance.domain.DO.LlmMemory;
import com.lyh.finance.domain.DO.LlmMemoryVector;
import com.lyh.finance.enums.MessageType;
import com.lyh.finance.memory.MemoryQuery;
import com.lyh.finance.model.embedding.EmbeddingModel;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.IDs;
import io.milvus.grpc.LongArray;
import io.milvus.grpc.SearchResultData;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusMemoryRepository implements IMemoryRepository<LlmMemory, Long> {
    @Value("${milvus.collection-name:llm_memory_vectors}")
    private String collectionName;
    @Autowired
    private MilvusServiceClient milvusClient;
    private final EmbeddingModel embeddingModel;

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
            vector.setContent_vector(embeddingModel.genVector(it.getContent()));
            vectorList.add(vector);
        }
        if (CollectionUtils.isEmpty(vectorList)) {
            return Collections.emptyList();
        }
        insertBatch(vectorList);
        log.info("成功保存到向量数据库，条数：{}", vectorList.size());
        return vectorList.stream().map(LlmMemoryVector::getId).collect(Collectors.toList());
    }

    @Override
    public List<Long> get(MemoryQuery query) {
        List<Float> queryVector = embeddingModel.genVector(query.getQuery());
        List<List<Float>> queryVectors = Collections.singletonList(queryVector);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("content_vector")
                .withFloatVectors(queryVectors)
                .withLimit((long) query.getLimit())
                .withMetricType(MetricType.COSINE)
                .withExpr(String.format("conversation_id == '%s'", query.getConversationId()))
                .withParams(String.format("{\"radius\": %s, \"range_filter\": 1.0}", query.getMinScore()))
                .withOutFields(Arrays.asList("id", "type"))
                .build();
        R<SearchResults> res = milvusClient.search(searchParam);
        if (res.getData() == null) {
            return Collections.emptyList();
        }
        return Optional.of(res)
                .map(R::getData)
                .map(SearchResults::getResults)
                .map(SearchResultData::getIds)
                .map(IDs::getIntId)
                .map(LongArray::getDataList)
                .orElse(Collections.emptyList());
    }

    /**
     * 批量插入
     *
     * @param list
     * @return
     * @throws IllegalAccessException
     */
    private int insertBatch(List<LlmMemoryVector> list) {
        if (CollectionUtils.isEmpty(list)) {
            return 0;
        }
        Field[] fields = list.get(0).getClass().getDeclaredFields();
        List<InsertParam.Field> collFields = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            List<Object> columnValues = new ArrayList<>();
            try {
                for (LlmMemoryVector obj : list) {
                    columnValues.add(field.get(obj));
                }
            } catch (Exception e) {
                log.error("获取字段值异常，字段：{}", field);
            }
            InsertParam.Field collField = new InsertParam.Field(field.getName(), columnValues);
            collFields.add(collField);
        }
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(collFields)
                .build();
        milvusClient.insert(insertParam);
        return list.size();
    }
}
