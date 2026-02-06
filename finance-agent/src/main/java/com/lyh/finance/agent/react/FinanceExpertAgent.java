package com.lyh.finance.agent.react;

import com.lyh.finance.agent.ReActAgent;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public class FinanceExpertAgent extends ReActAgent {
    public FinanceExpertAgent(ChatModel chatModel,
                              MemoryManager memoryManager,
                              ToolManager toolManager
    ) {
        super(chatModel, memoryManager, toolManager);
    }

    @Override
    public SystemMessage systemMessage() {
        SystemMessage systemMessage = super.systemMessage();
        return new SystemMessage(systemMessage.getContent().replace("{cur_date_time}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())));
    }
}
