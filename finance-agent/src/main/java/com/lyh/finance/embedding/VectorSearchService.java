package com.lyh.finance.embedding;

import com.lyh.finance.domain.DO.LlmMemoryVector;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.IDs;
import io.milvus.grpc.LongArray;
import io.milvus.grpc.SearchResultData;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author lengYinHui
 * @date 2026/2/4
 */
@Slf4j
@Service
public class VectorSearchService {
    @Value("${milvus.collection-name:llm_memory_vectors}")
    private String collectionName;
    @Autowired
    private MilvusServiceClient milvusClient;
    @Autowired
    private GeminiEmbed geminiEmbed;

    /**
     * 批量插入
     *
     * @param list
     * @return
     * @throws IllegalAccessException
     */
    public int insertBatch(List<LlmMemoryVector> list) {
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

    /**
     * 向量相似度搜索（Top-K）
     *
     * @param text
     * @param topK
     * @return
     */
    public List<Long> searchSimilarVectors(String conversationId, String text, int topK) {
        List<Float> queryVector = geminiEmbed.genVector(text);
        List<List<Float>> queryVectors = Collections.singletonList(queryVector);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("content_vector")
                .withFloatVectors(queryVectors)
                .withLimit((long) topK)
                .withMetricType(MetricType.COSINE)
                .withExpr(String.format("conversation_id == '%s'", conversationId))
                .withParams("{\"radius\": 0.85, \"range_filter\": 1.0}")
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
}
