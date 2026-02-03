package com.lyh.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@MapperScan({"com.lyh.trade.self_react.mapper"})
@SpringBootApplication(scanBasePackages = "com.lyh.trade.**")
public class TradeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeAgentApplication.class, args);
    }
}
