package com.lyh.finance.config;

import com.lyh.base.agent.mcp.McpClientManager;
import com.lyh.base.agent.skills.SkillsLoader;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.finance.tools.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ToolConfig
 *
 * @author lcry
 * @since 2025/04/22 21:08
 */
@Component
public class ToolConfig {

    @Autowired(required = false)
    private McpClientManager mcpClientManager;

    @Bean
    public ToolBuilder financeExpertAgentTools(
            SkillsLoader skillsLoader,
            DateTimeTool dateTimeTool,
            StockTool stockTool,
            FundTool fundTool,
            SearchTool searchTool,
            AkShareTool akShareTool
    ) {
        ToolBuilder builder = new ToolBuilder(skillsLoader, dateTimeTool, stockTool, fundTool, searchTool, akShareTool);
        if (mcpClientManager != null) {
            mcpClientManager.registerAllTools(builder);
        }
        return builder;
    }

    @Bean
    public ToolBuilder investorAgentTools(
            DateTimeTool dateTimeTool,
            AccountTool accountTool
    ) {
        return new ToolBuilder(dateTimeTool, accountTool);
    }

    @Bean
    public ToolBuilder technicalAnalysisTools(
            DateTimeTool dateTimeTool,
            AkShareTool akShareTool
    ) {
        return new ToolBuilder(dateTimeTool, akShareTool);
    }

    @Bean
    public ToolBuilder fundamentalAnalysisTools(
            DateTimeTool dateTimeTool,
            AkShareTool akShareTool
    ) {
        return new ToolBuilder(dateTimeTool, akShareTool);
    }

    @Bean
    public ToolBuilder marketSentimentTools(
            DateTimeTool dateTimeTool,
            SearchTool searchTool,
            AkShareTool akShareTool
    ) {
        return new ToolBuilder(dateTimeTool, searchTool, akShareTool);
    }

    @Bean
    public ToolBuilder riskAssessmentTools(
            DateTimeTool dateTimeTool
    ) {
        return new ToolBuilder(dateTimeTool);
    }

    @Bean
    public ToolBuilder summaryReportTools(
            DateTimeTool dateTimeTool
    ) {
        return new ToolBuilder(dateTimeTool);
    }

    @Bean
    public ToolBuilder coordinatorAgentTools(
            SubAgentInvokeTool subAgentInvokeTool,
            DateTimeTool dateTimeTool
    ) {
        return new ToolBuilder(subAgentInvokeTool, dateTimeTool);
    }
}