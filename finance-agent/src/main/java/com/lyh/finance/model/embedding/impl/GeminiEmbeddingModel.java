package com.lyh.finance.model.embedding.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lyh.finance.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Component
public class GeminiEmbeddingModel extends EmbeddingModel {
    public static final String EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=AIzaSyCOUdoD13IVOgvYV6zZaEh7Eypns4ypo2M";
    @Resource
    private RestTemplate proxyRestTemplate;

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
        EmbeddingResp embeddingResp = proxyRestTemplate.postForObject(EMBEDDING_URL, params,
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
