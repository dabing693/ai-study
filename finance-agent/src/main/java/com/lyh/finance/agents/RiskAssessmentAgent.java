package com.lyh.finance.agents;

import com.lyh.base.agent.define.SimpleAgent;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

/**
 * 风险评估 Agent
 * 负责基于前面的分析结果进行风险评估
 */
public class RiskAssessmentAgent extends SimpleAgent {

    public RiskAssessmentAgent(ChatModel chatModel,
                                ToolManager toolManager) {
        super(chatModel, toolManager);
    }
}
