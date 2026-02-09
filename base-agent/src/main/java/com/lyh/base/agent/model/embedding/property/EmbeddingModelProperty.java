package com.lyh.base.agent.model.embedding.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "embedding")
@Data
public class EmbeddingModelProperty {
    protected String provider;
    protected String baseUrl;
    protected String apiKey;
    protected String model;
}
