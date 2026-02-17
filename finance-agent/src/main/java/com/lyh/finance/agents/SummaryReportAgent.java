package com.lyh.finance.agents;

import com.lyh.base.agent.define.SimpleAgent;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

/**
 * 总结报告 Agent
 * 负责整合所有分析结果，生成最终报告
 */
public class SummaryReportAgent extends SimpleAgent {

    public SummaryReportAgent(ChatModel chatModel,
                               ToolManager toolManager) {
        super(chatModel, toolManager);
    }
}
