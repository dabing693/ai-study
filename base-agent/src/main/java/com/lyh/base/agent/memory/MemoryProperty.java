package com.lyh.base.agent.memory;


import com.lyh.base.agent.enums.MemoryStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "memory")
public class MemoryProperty {
    private Integer maxMessageNum = 20;
    /**
     * @see MemoryStrategy
     */
    private String strategy = MemoryStrategy.sliding_window.name();
    private Double minScore = 0.90;

    /**
     * 是否开启记忆摘要压缩
     */
    private boolean enableSummary = false;
    /**
     * 触发摘要的条数阈值
     */
    private Integer summaryThreshold = 20;
    /**
     * 在执行摘要压缩时，要保留多少条最新消息不被压缩
     */
    private Integer activeWindow = 10;
}
