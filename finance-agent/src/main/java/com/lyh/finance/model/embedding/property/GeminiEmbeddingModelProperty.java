package com.lyh.finance.model.embedding.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding.gemini")
@Data
public class GeminiEmbeddingModelProperty extends EmbeddingModelProperty {
    public GeminiEmbeddingModelProperty() {
        this.baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";
    }
}
