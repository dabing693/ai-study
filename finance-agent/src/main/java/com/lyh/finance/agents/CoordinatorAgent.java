package com.lyh.finance.agents;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.tool.ToolManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 核心规划大总管 Agent
 * 负责解析用户问题目标并按需调度不同的子专家 Agent（如基本面/技术面分析）。
 */
public class CoordinatorAgent extends ReActAgent {

    public CoordinatorAgent(ChatModel chatModel,
                            MemoryManager memoryManager,
                            ToolManager toolManager) {
        super(chatModel, memoryManager, toolManager);
    }

    @Override
    public SystemMessage systemMessage() {
        SystemMessage systemMessage = super.systemMessage();
        String content = systemMessage.getContent();
        if (content != null) {
            content = content.replace("{cur_date_time}",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        return new SystemMessage(content);
    }
}
