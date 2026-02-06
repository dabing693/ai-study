package com.lyh.finance.config;

import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.model.chat.property.ModelProperty;
import com.lyh.finance.model.chat.property.ZhiPuModelProperty;
import com.lyh.finance.model.chat.impl.ZhiPuChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@EnableConfigurationProperties({ZhiPuModelProperty.class})
@Configuration
public class ModelConfig {
    @Bean
    @ConditionalOnProperty(prefix = "model.zhipu", name = "api-key")
    public ChatModel chatModel(ModelProperty modelProperty,
                               RestTemplate restTemplate
    ) {
        return new ZhiPuChatModel(modelProperty, restTemplate);
    }
}
