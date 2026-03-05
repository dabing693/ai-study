package com.lyh.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@EnableAspectJAutoProxy
@MapperScan({"com.lyh.finance.mapper"})
@SpringBootApplication(scanBasePackages = "com.lyh.finance.**")
public class FinanceAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceAgentApplication.class, args);
    }
}
