package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 技术分析 Agent
 * 负责分析股票的技术面：K线、技术指标、趋势等
 */
public class TechnicalAnalysisAgent extends ReActAgent {

    public TechnicalAnalysisAgent(ChatModel chatModel,
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
