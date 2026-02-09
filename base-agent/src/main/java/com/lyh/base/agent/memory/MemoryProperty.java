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
}
