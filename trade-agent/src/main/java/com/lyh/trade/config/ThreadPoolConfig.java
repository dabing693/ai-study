package com.lyh.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Configuration
public class ThreadPoolConfig {
    @Bean
    public ExecutorService milvusThreadPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
