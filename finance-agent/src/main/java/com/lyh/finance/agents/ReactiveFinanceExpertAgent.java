package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReactiveReActAgent;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

public class ReactiveFinanceExpertAgent extends ReactiveReActAgent {
    public ReactiveFinanceExpertAgent(ChatModel chatModel, MemoryManager memoryManager, ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }
}
