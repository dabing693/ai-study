package com.lyh.finance.config;

import com.lyh.finance.agent.react.FinanceExpertAgent;
import com.lyh.finance.agent.simple.InvestorAgent;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolBuilder;
import com.lyh.finance.tool.ToolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Configuration
public class AgentConfig {
    @Bean
    public FinanceExpertAgent financeExpertAgent(ChatModel chatModel,
                                                 MemoryManager memoryManager,
                                                 ToolBuilder financeExpertAgentTools) {
        ToolManager toolManager = financeExpertAgentTools.buildToolManager();
        return new FinanceExpertAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public InvestorAgent investorAgent(ChatModel chatModel,
                                       ToolBuilder investorAgentTools) {
        ToolManager toolManager = investorAgentTools.buildToolManager();
        return new InvestorAgent(chatModel, toolManager);
    }
}
