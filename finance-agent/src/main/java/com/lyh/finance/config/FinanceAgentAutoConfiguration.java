package com.lyh.finance.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan("com.lyh.finance.mapper")
@ComponentScan({
        "com.lyh.finance.tools",
        "com.lyh.finance.service",
        "com.lyh.finance.util",
})
@Import({
        ToolConfig.class
})
public class FinanceAgentAutoConfiguration {
}
