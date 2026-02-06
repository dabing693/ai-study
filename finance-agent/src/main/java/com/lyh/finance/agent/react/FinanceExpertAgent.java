package com.lyh.finance.agent.react;

import com.lyh.finance.agent.ReActAgent;
import com.lyh.finance.agent.property.AgentProperty;
import com.lyh.finance.agent.property.ReActAgentProperty;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.tool.ToolManager;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Component
public class FinanceExpertAgent extends ReActAgent {
    public FinanceExpertAgent(ChatModel chatModel,
                              MemoryManager memoryManager,
                              ToolManager toolManager,
                              ReActAgentProperty agentProperty
    ) {
        super(chatModel, memoryManager, toolManager, agentProperty);
    }

    @Override
    public SystemMessage systemMessage() {
        SystemMessage systemMessage = super.systemMessage();
        return new SystemMessage(systemMessage.getContent().replace("{cur_date_time}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())));
    }
}
