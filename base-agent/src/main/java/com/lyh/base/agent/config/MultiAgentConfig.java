package com.lyh.base.agent.config;

import com.lyh.base.agent.orchestrator.AgentRegistry;
import com.lyh.base.agent.orchestrator.MultiAgentOrchestrator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class MultiAgentConfig {
    @Bean
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();
    }

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(1);
    }

    @Bean
    public MultiAgentOrchestrator multiAgentOrchestrator(AgentRegistry agentRegistry,
                                                         ExecutorService executorService) {
        return new MultiAgentOrchestrator(agentRegistry, executorService);
    }
}
