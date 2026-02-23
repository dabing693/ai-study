package com.lyh.finance.config;

import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolBuilder;
import com.lyh.base.agent.tool.ToolManager;
import com.lyh.finance.agents.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@RequiredArgsConstructor
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

    @Bean
    public TechnicalAnalysisAgent technicalAnalysisAgent(ChatModel chatModel,
                                                         MemoryManager memoryManager,
                                                         ToolBuilder technicalAnalysisTools) {
        ToolManager toolManager = technicalAnalysisTools.buildToolManager();
        return new TechnicalAnalysisAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public FundamentalAnalysisAgent fundamentalAnalysisAgent(ChatModel chatModel,
                                                             MemoryManager memoryManager,
                                                             ToolBuilder fundamentalAnalysisTools) {
        ToolManager toolManager = fundamentalAnalysisTools.buildToolManager();
        return new FundamentalAnalysisAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public MarketSentimentAgent marketSentimentAgent(ChatModel chatModel,
                                                     MemoryManager memoryManager,
                                                     ToolBuilder marketSentimentTools) {
        ToolManager toolManager = marketSentimentTools.buildToolManager();
        return new MarketSentimentAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public RiskAssessmentAgent riskAssessmentAgent(ChatModel chatModel,
                                                   MemoryManager memoryManager,
                                                   ToolBuilder riskAssessmentTools) {
        ToolManager toolManager = riskAssessmentTools.buildToolManager();
        return new RiskAssessmentAgent(chatModel, memoryManager, toolManager);
    }

    @Bean
    public SummaryReportAgent summaryReportAgent(ChatModel chatModel,
                                                 MemoryManager memoryManager,
                                                 ToolBuilder summaryReportTools) {
        ToolManager toolManager = summaryReportTools.buildToolManager();
        return new SummaryReportAgent(chatModel, memoryManager, toolManager);
    }
}
