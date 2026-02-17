package com.lyh.finance.config;

import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.finance.tools.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * ToolConfig
 *
 * @author lcry
 * @since 2025/04/22 21:08
 */
@Component
public class ToolConfig {
    @Bean
    public ToolBuilder financeExpertAgentTools(
            DateTimeTool dateTimeTool,
            AccountTool accountTool,
            StockTool stockTool,
            FundTool fundTool,
            SearchTool searchTool,
            AkShareTool akShareTool
    ) {
        return new ToolBuilder(dateTimeTool, accountTool, stockTool, fundTool, searchTool, akShareTool);
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
}