package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

/**
 * 总结报告 Agent
 * 负责整合所有分析结果，生成最终报告
 */
public class SummaryReportAgent extends ReActAgent {
    public SummaryReportAgent(ChatModel chatModel, MemoryManager memoryManager, ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }
}
