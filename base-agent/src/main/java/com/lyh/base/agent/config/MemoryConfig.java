package com.lyh.base.agent.config;

import com.lyh.base.agent.handler.QueryRewriteModelHandler;
import com.lyh.base.agent.handler.SummaryModelHandler;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.base.agent.mapper.LlmTurnMapper;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.memory.MemoryProperty;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import com.lyh.base.agent.memory.repository.RamMemoryRepository;
import com.lyh.base.agent.memory.repository.RedisMemoryRepository;
import com.lyh.base.agent.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.memory.ReactiveMemoryManager;
import com.lyh.base.agent.memory.repository.SummaryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.ExecutorService;

public class MemoryConfig {
    @Bean
    @ConditionalOnMissingBean
    public MemoryManager memoryManager(MemoryProperty memoryProperty,
                                       MysqlMemoryRepository mysqlMemoryRepository,
                                       RedisMemoryRepository redisMemoryRepository,
                                       MilvusMemoryRepository milvusMemoryRepository,
                                       SummaryRepository summaryRepository,
                                       @Qualifier("milvusThreadPool") ExecutorService milvusThreadPool,
                                       SummaryModelHandler summaryModelHandler,
                                       QueryRewriteModelHandler queryRewriteModelHandler
    ) {
        return new MemoryManager(
                memoryProperty, mysqlMemoryRepository, redisMemoryRepository,
                milvusMemoryRepository, summaryRepository, milvusThreadPool,
                summaryModelHandler, queryRewriteModelHandler
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveMemoryManager reactiveMemoryManager(MemoryProperty memoryProperty,
                                                       MysqlMemoryRepository mysqlMemoryRepository,
                                                       MilvusMemoryRepository milvusMemoryRepository,
                                                       SummaryRepository summaryRepository,
                                                       @Qualifier("milvusThreadPool") ExecutorService milvusThreadPool,
                                                       ChatModel chatModel
    ) {
        return new ReactiveMemoryManager(memoryProperty, mysqlMemoryRepository, milvusMemoryRepository, summaryRepository, milvusThreadPool, chatModel);
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
    public SummaryRepository summaryRepository(com.lyh.base.agent.mapper.LlmSummaryMapper llmSummaryMapper) {
        return new SummaryRepository(llmSummaryMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MysqlMemoryRepository mysqlMemoryRepository(LlmMemoryMapper llmMemoryMapper, LlmTurnMapper llmTurnMapper) {
        return new MysqlMemoryRepository(llmMemoryMapper, llmTurnMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MilvusMemoryRepository milvusMemoryRepository(MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel) {
        return new MilvusMemoryRepository(milvusClientV2, embeddingModel);
    }

    @Bean
    public RedisMemoryRepository redisMemoryRepository(RedisTemplate<String, String> redisTemplate,
                                                       MemoryProperty memoryProperty) {
        return new RedisMemoryRepository(redisTemplate, memoryProperty);
    }
}
