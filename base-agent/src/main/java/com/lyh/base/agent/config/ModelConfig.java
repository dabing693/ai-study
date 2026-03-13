package com.lyh.base.agent.config;

import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.model.chat.property.ChatModelProperty;
import com.lyh.base.agent.model.chat.property.IflowChatModelProperty;
import com.lyh.base.agent.model.chat.property.ZhiPuChatModelProperty;
import com.lyh.base.agent.model.chat.impl.IflowChatModel;
import com.lyh.base.agent.model.chat.impl.ZhiPuChatModel;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import com.lyh.base.agent.model.embedding.impl.GeminiEmbeddingModel;
import com.lyh.base.agent.model.embedding.impl.OllamaEmbeddingModel;
import com.lyh.base.agent.model.embedding.property.EmbeddingModelProperty;
import com.lyh.base.agent.model.embedding.property.GeminiEmbeddingModelProperty;
import com.lyh.base.agent.model.embedding.property.OllamaEmbeddingModelProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@EnableConfigurationProperties({ZhiPuChatModelProperty.class,
        IflowChatModelProperty.class,
        EmbeddingModelProperty.class,
        GeminiEmbeddingModelProperty.class,
        OllamaEmbeddingModelProperty.class
})
@Configuration
public class ModelConfig {
    @Primary
    @Bean
    @ConditionalOnProperty(prefix = "model.provider.chat", name = "default", havingValue = "zhipu")
    public ChatModel zhiPuChatModel(ZhiPuChatModelProperty chatModelProperty,
                                    RestTemplate restTemplate) {
        return new ZhiPuChatModel(chatModelProperty, restTemplate);
    }

    @Primary
    @Bean
    @ConditionalOnProperty(prefix = "model.provider.chat", name = "default", havingValue = "iflow")
    public ChatModel iflowChatModel(IflowChatModelProperty chatModelProperty,
                                    RestTemplate restTemplate) {
        return new IflowChatModel(chatModelProperty, restTemplate);
    }

    @Bean
    public ChatModel queryRewriteModel(ZhiPuChatModelProperty chatModelProperty,
                                       RestTemplate restTemplate) {
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
