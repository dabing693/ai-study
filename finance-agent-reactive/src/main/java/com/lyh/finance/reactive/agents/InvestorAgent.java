package com.lyh.finance.reactive.agents;

import com.lyh.base.agent.define.SimpleAgent;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public class InvestorAgent extends SimpleAgent {
    public InvestorAgent(ChatModel chatModel,
                         ToolManager toolManager) {
        super(chatModel, toolManager);
    }
}
