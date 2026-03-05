package com.lyh.base.agent.config;

import com.lyh.base.agent.context.SpringContext;
import com.lyh.base.agent.observation.LangfuseConfig;
import com.lyh.base.agent.observation.LangfuseOpenTelemetryService;
import com.lyh.base.agent.observation.LangfuseTraceAspect;
import com.lyh.base.agent.observation.LangfuseTraceService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan("com.lyh.base.agent.mapper")
@Import({
        LangfuseTraceAspect.class,
        LangfuseTraceService.class,
        LangfuseOpenTelemetryService.class,
        SpringContext.class,
        MemoryConfig.class,
        MilvusConfig.class,
        ModelConfig.class,
        RestTemplateConfig.class,
        ThreadPoolConfig.class,
        MultiAgentConfig.class,
        LangfuseConfig.class,
})
public class BaseAgentAutoConfiguration {
}
