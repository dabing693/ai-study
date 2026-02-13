package com.lyh.finance.reactive.config;

import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tool.ToolManager;
import com.lyh.finance.reactive.agents.ReactiveFinanceExpertAgent;
import com.lyh.finance.reactive.agents.InvestorAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Configuration
public class AgentConfig {
    @Bean
    public ReactiveFinanceExpertAgent reactiveFinanceExpertAgent(ChatModel chatModel,
                                                         MemoryManager memoryManager,
                                                         ToolBuilder financeExpertAgentTools) {
        ToolManager toolManager = financeExpertAgentTools.buildToolManager();
        return new ReactiveFinanceExpertAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public InvestorAgent investorAgent(ChatModel chatModel,
                                       ToolBuilder investorAgentTools) {
        ToolManager toolManager = investorAgentTools.buildToolManager();
        return new InvestorAgent(chatModel, toolManager);
    }
}
