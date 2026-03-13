package com.lyh.base.agent.config;

import com.lyh.base.agent.context.SpringContext;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.observation.LangfuseConfig;
import com.lyh.base.agent.observation.LangfuseOpenTelemetryService;
import com.lyh.base.agent.observation.LangfuseTraceAspect;
import com.lyh.base.agent.observation.LangfuseTraceService;
import com.lyh.base.agent.skills.SkillInvoker;
import com.lyh.base.agent.skills.SkillsLoader;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tool.ToolInvoker;
import com.lyh.base.agent.tool.ToolManager;
import com.lyh.base.agent.memory.MemoryManager;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan({
        "com.lyh.base.agent.mapper",
})
@ComponentScan({
        "com.lyh.base.agent.handler",
})
@Import({
        SkillsLoader.class,
        LangfuseTraceAspect.class,
        LangfuseTraceService.class,
        LangfuseOpenTelemetryService.class,
        ToolInvoker.class,
        SpringContext.class,
        MemoryConfig.class,
        MilvusConfig.class,
        ModelConfig.class,
        RestTemplateConfig.class,
        ThreadPoolConfig.class,
        MultiAgentConfig.class,
        LangfuseConfig.class,
        SystemToolConfig.class,
        RedisConfig.class,
})
public class BaseAgentAutoConfiguration {
    @Bean
    public SkillInvoker skillInvoker(ChatModel chatModel,
                                     ToolBuilder skillsLoader,
                                     MemoryManager memoryManager) {
        ToolManager toolManager = skillsLoader.buildToolManager();
        return new SkillInvoker(chatModel, toolManager, memoryManager);
    }
}
