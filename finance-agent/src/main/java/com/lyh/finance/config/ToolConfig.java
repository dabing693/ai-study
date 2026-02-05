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
    public ToolBuilder myTools(
            WeatherService weatherService,
            AccountService accountService,
            StockService stockService,
            FundService fundService,
            SearchService searchService,
            DateTimeTool dateTimeTool
    ) {
        return new ToolBuilder(weatherService, accountService, stockService, fundService, searchService,
                dateTimeTool);
    }
}