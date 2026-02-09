package com.lyh.base.agent.model.embedding.impl;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import com.lyh.base.agent.model.embedding.property.OllamaEmbeddingModelProperty;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ollama Embedding 模型实现
 *
 * @author lengYinHui
 */
public class OllamaEmbeddingModel extends EmbeddingModel {

    @Resource
    private RestTemplate restTemplate;

    private final OllamaEmbeddingModelProperty ollamaProperty;

    public OllamaEmbeddingModel(OllamaEmbeddingModelProperty ollamaProperty) {
        super(ollamaProperty);
        this.ollamaProperty = ollamaProperty;
    }

    /**
     * Ollama API 生成向量
     *
     * @param content 文本内容
     * @return 向量列表
     */
    @Override
    public List<Float> genVector(String content) {
        JSONObject params = new JSONObject()
                .fluentPut("model", ollamaProperty.getModel())
                .fluentPut("prompt", content);

        EmbeddingResp response = restTemplate.postForObject(
                ollamaProperty.getBaseUrl(),
                params,
                EmbeddingResp.class
        );

        return Optional.ofNullable(response)
                .map(EmbeddingResp::getEmbedding)
                .orElse(new ArrayList<>());
    }

    @Data
    public static class EmbeddingResp {
        private List<Float> embedding;
    }
}
