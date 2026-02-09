package com.lyh.base.agent.config;

import com.lyh.base.agent.context.SpringContext;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan("com.lyh.base.agent.mapper")
@Import({
        SpringContext.class,
        MemoryConfig.class,
        MilvusConfig.class,
        ModelConfig.class,
        RestTemplateConfig.class,
        ThreadPoolConfig.class
})
public class BaseAgentAutoConfiguration {
}
