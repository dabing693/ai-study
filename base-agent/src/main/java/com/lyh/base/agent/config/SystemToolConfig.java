package com.lyh.base.agent.config;

import com.lyh.base.agent.skills.SkillsLoader;
import com.lyh.base.agent.tool.ToolBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemToolConfig {
    @Bean
    public ToolBuilder skillsLoader(
            SkillsLoader skillsLoader
    ) {
        return new ToolBuilder(skillsLoader);
    }
}
