package com.lyh.finance.model.embedding.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding.ollama")
@Data
public class OllamaEmbeddingModelProperty extends EmbeddingModelProperty {
    public OllamaEmbeddingModelProperty() {
        this.model = "qwen3-embedding:0.6b";
    }
}
