package com.lyh.base.agent.config;

import com.lyh.base.agent.skills.SkillsLoader;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tools.PythonREPLToolAdapter;
import com.lyh.base.agent.tools.BashTool;
import com.lyh.base.agent.tools.LocalFileReadTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemToolConfig {
    @Bean
    public PythonREPLToolAdapter pythonREPLTool() {
        return new PythonREPLToolAdapter();
    }

    @Bean
    public BashTool bashTool() {
        return new BashTool();
    }

    @Bean
    public LocalFileReadTool localFileReadTool() {
        return new LocalFileReadTool();
    }

    @Bean
    public ToolBuilder skillsLoader(
            SkillsLoader skillsLoader,
            PythonREPLToolAdapter pythonREPLTool,
            BashTool bashTool,
            LocalFileReadTool localFileReadTool) {
        return new ToolBuilder(skillsLoader, pythonREPLTool, bashTool, localFileReadTool);
    }
}
