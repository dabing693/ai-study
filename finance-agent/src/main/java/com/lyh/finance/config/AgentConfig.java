package com.lyh.finance.config;

import com.lyh.finance.agent.property.FinanceExpertAgentProperty;
import com.lyh.finance.agent.property.InvestorAgentProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@EnableConfigurationProperties(value = {
        FinanceExpertAgentProperty.class,
        InvestorAgentProperty.class
        })
@Configuration
public class AgentConfig {
}
