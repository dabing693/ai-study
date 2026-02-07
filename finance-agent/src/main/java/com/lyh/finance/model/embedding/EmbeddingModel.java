package com.lyh.finance.model.embedding;

import com.lyh.finance.model.embedding.property.EmbeddingModelProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
@RequiredArgsConstructor
public abstract class EmbeddingModel {
    protected final EmbeddingModelProperty embeddingModelProperty;
    public abstract List<Float> genVector(String content);
}
