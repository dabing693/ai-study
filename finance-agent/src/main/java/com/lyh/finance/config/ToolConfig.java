package com.lyh.finance.config;

import com.lyh.finance.tool.ToolBuilder;
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
            SearchTool searchTool
    ) {
        return new ToolBuilder(dateTimeTool, accountTool, stockTool, fundTool, searchTool);
    }

    @Bean
    public ToolBuilder investorAgentTools(
            DateTimeTool dateTimeTool,
            AccountTool accountTool
    ) {
        return new ToolBuilder(dateTimeTool, accountTool);
    }
}