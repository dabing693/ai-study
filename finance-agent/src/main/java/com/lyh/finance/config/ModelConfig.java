package com.lyh.finance.config;

import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.ChatModel;
import com.lyh.finance.model.config.ModelProperty;
import com.lyh.finance.model.config.ZhiPuModelProperty;
import com.lyh.finance.model.impl.ZhiPuChatModel;
import com.lyh.finance.tool.ToolBuilder;
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
    @ConditionalOnProperty(prefix = "model.zhipu", name = "base-url")
    public ChatModel chatModel(ModelProperty modelProperty,
                               RestTemplate restTemplate,
                               MemoryManager memoryManager,
                               ToolBuilder toolBuilder
    ) {
        return new ZhiPuChatModel(modelProperty, restTemplate, memoryManager, toolBuilder);
    }
}
