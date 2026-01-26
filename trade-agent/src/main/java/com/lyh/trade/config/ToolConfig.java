package com.lyh.trade.config;



import com.lyh.trade.tools.AccountService;
import com.lyh.trade.tools.FundService;
import com.lyh.trade.tools.StockService;
import com.lyh.trade.tools.WeatherService;
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
    public ToolCallbackProvider myTools(
            WeatherService weatherService,
            AccountService accountService,
            StockService stockService,
            FundService fundService
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherService, accountService, stockService, fundService)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}