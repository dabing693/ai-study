package com.lyh.finance.config;

import com.lyh.finance.agents.FinanceExpertAgent;
import com.lyh.finance.agents.InvestorAgent;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tool.ToolManager;
import com.lyh.finance.agents.ReactiveFinanceExpertAgent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Configuration
public class AgentConfig {
    @Bean
    @ConditionalOnProperty(prefix = "react.chat.stream.use", name = "reactive", havingValue = "false")
    public FinanceExpertAgent financeExpertAgent(ChatModel chatModel,
                                                 MemoryManager memoryManager,
                                                 ToolBuilder financeExpertAgentTools) {
        ToolManager toolManager = financeExpertAgentTools.buildToolManager();
        return new FinanceExpertAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    @ConditionalOnProperty(prefix = "react.chat.stream.use", name = "reactive", havingValue = "true", matchIfMissing = true)
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
