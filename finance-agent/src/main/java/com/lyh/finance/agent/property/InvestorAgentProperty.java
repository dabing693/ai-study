package com.lyh.finance.agent.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Data
@ConfigurationProperties(prefix = "agent.investor")
public class InvestorAgentProperty extends SimpleAgentProperty {
}
