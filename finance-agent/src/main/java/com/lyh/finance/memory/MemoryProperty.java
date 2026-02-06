package com.lyh.finance.memory;


import com.lyh.finance.enums.MemoryStrategy;
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
     * @see com.lyh.finance.enums.MemoryStrategy
     */
    private String strategy = MemoryStrategy.sliding_window.name();
    private Double minScore = 0.85;
}
