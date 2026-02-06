package com.lyh.finance.model.embedding;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public abstract class EmbeddingModel {
    public abstract List<Float> genVector(String content);
}
