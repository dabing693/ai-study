package com.lyh.base.agent.model.embedding.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import com.lyh.base.agent.model.embedding.property.EmbeddingModelProperty;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
public class GeminiEmbeddingModel extends EmbeddingModel {
    @Resource
    private RestTemplate proxyRestTemplate;

    public GeminiEmbeddingModel(EmbeddingModelProperty embeddingModelProperty) {
        super(embeddingModelProperty);
    }

    /**
     * gemini api生成向量
     *
     * @param content
     * @return
     */
    public List<Float> genVector(String content) {
        JSONObject params = new JSONObject()
                .fluentPut("content", new JSONObject().fluentPut("parts", new JSONArray().fluentAdd(
                        new JSONObject().fluentPut("text", content)
                )))
                .fluentPut("outputDimensionality", 1024)
                .fluentPut("taskType", "RETRIEVAL_DOCUMENT");
        String url = String.format("%s?key=%s", embeddingModelProperty.getBaseUrl(), embeddingModelProperty.getApiKey());
        EmbeddingResp embeddingResp = proxyRestTemplate.postForObject(url, params,
                EmbeddingResp.class);
        return Optional.of(embeddingResp).map(EmbeddingResp::getEmbedding)
                .map(EmbeddingResp.Embedding::getValues).orElse(new ArrayList<>());
    }

    @Data
    public static class EmbeddingResp {
        private Embedding embedding;

        @Data
        public static class Embedding {
            private List<Float> values;
        }
    }
}
