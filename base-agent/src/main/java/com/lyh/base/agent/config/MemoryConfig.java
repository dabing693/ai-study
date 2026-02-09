package com.lyh.base.agent.config;

import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.memory.MemoryProperty;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import com.lyh.base.agent.memory.repository.RamMemoryRepository;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;

public class MemoryConfig {
    @Bean
    @ConditionalOnMissingBean
    public MemoryManager memoryManager(MemoryProperty memoryProperty,
                                       MysqlMemoryRepository mysqlMemoryRepository,
                                       MilvusMemoryRepository milvusMemoryRepository,
                                       ExecutorService milvusThreadPoo
    ) {
        return new MemoryManager(memoryProperty, mysqlMemoryRepository, milvusMemoryRepository, milvusThreadPoo);
    }

    @Bean
    @ConditionalOnMissingBean
    public MemoryProperty memoryProperty() {
        return new MemoryProperty();
    }

    @Bean
    @ConditionalOnMissingBean
    public RamMemoryRepository ramMemoryRepository() {
        return new RamMemoryRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public MysqlMemoryRepository mysqlMemoryRepository(LlmMemoryMapper llmMemoryMapper) {
        return new MysqlMemoryRepository(llmMemoryMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MilvusMemoryRepository milvusMemoryRepository(MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel) {
        return new MilvusMemoryRepository(milvusClientV2, embeddingModel);
    }
}
