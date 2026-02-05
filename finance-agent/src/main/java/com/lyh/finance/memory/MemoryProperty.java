package com.lyh.finance.memory;


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
    private Integer maxMessageNum;
    /**
     * @see com.lyh.finance.enums.MemoryStrategy
     */
    private String strategy;
}
