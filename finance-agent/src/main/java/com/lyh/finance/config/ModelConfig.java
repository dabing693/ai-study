package com.lyh.finance.config;

import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.model.chat.property.ChatModelProperty;
import com.lyh.finance.model.chat.property.ZhiPuChatModelProperty;
import com.lyh.finance.model.chat.impl.ZhiPuChatModel;
import com.lyh.finance.model.embedding.EmbeddingModel;
import com.lyh.finance.model.embedding.impl.GeminiEmbeddingModel;
import com.lyh.finance.model.embedding.impl.OllamaEmbeddingModel;
import com.lyh.finance.model.embedding.property.EmbeddingModelProperty;
import com.lyh.finance.model.embedding.property.GeminiEmbeddingModelProperty;
import com.lyh.finance.model.embedding.property.OllamaEmbeddingModelProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@EnableConfigurationProperties({ZhiPuChatModelProperty.class,
        EmbeddingModelProperty.class,
        GeminiEmbeddingModelProperty.class,
        OllamaEmbeddingModelProperty.class
})
@Configuration
public class ModelConfig {
    @Bean
    @ConditionalOnProperty(prefix = "model.zhipu", name = "api-key")
    public ChatModel chatModel(ChatModelProperty chatModelProperty,
                               RestTemplate restTemplate
    ) {
        return new ZhiPuChatModel(chatModelProperty, restTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "gemini", matchIfMissing = true)
    public EmbeddingModel geminiEmbeddingModel(GeminiEmbeddingModelProperty embeddingModelProperty) {
        return new GeminiEmbeddingModel(embeddingModelProperty);
    }

    @Bean
    @ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel(OllamaEmbeddingModelProperty ollamaProperty) {
        return new OllamaEmbeddingModel(ollamaProperty);
    }
}
