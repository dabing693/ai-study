package com.lyh.finance.agent.simple;

import com.lyh.finance.agent.SimpleAgent;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;

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
