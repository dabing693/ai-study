package com.lyh.trade.config;


import com.lyh.trade.tools.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

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
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate proxyRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
        factory.setProxy(proxy);
        return new RestTemplate(factory);
    }
}