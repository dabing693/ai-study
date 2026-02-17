package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 基本面分析 Agent
 * 负责分析股票的基本面：财报、估值、成长性等
 */
public class FundamentalAnalysisAgent extends ReActAgent {

    public FundamentalAnalysisAgent(ChatModel chatModel,
                                      MemoryManager memoryManager,
                                      ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }

    @Override
    public SystemMessage systemMessage() {
        SystemMessage systemMessage = super.systemMessage();
        return new SystemMessage(systemMessage.getContent().replace("{cur_date_time}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())));
    }
}
