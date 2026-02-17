package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 市场情绪分析 Agent
 * 负责分析市场情绪：新闻、舆情、资金流向等
 */
public class MarketSentimentAgent extends ReActAgent {

    public MarketSentimentAgent(ChatModel chatModel,
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
