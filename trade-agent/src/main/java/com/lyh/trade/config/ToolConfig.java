package com.lyh.trade.config;


import com.lyh.trade.self_react.ToolBuilder;
import com.lyh.trade.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * ToolConfig
 *
 * @author lcry
 * @since 2025/04/22 21:08
 */
@Component
public class ToolConfig {

    @Bean
    public ToolCallbackProvider springAITools(
            WeatherService weatherService,
            AccountService accountService,
            StockService stockService,
            FundService fundService,
            SearchService searchService,
            DateTimeTool dateTimeTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService, accountService, stockService, fundService, searchService,
                        dateTimeTool)
                .build();
    }

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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}