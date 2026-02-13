package com.lyh.finance.reactive.agents;

import com.lyh.base.agent.define.ReactiveReActAgent;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
public class ReactiveFinanceExpertAgent extends ReactiveReActAgent {
    public ReactiveFinanceExpertAgent(ChatModel chatModel,
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
