package com.lyh.finance.agent.simple;

import com.lyh.finance.agent.SimpleAgent;
import com.lyh.finance.agent.property.SimpleAgentProperty;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import org.springframework.stereotype.Component;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Component
public class InvestorAgent extends SimpleAgent {
    public InvestorAgent(ChatModel chatModel,
                         MemoryManager memoryManager,
                         ToolManager toolManager,
                         SimpleAgentProperty agentProperty) {
        super(chatModel, memoryManager, toolManager, agentProperty);
    }
}
