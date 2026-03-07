package com.lyh.base.agent.skills;

import com.lyh.base.agent.define.SimpleAgent;
import com.lyh.base.agent.domain.ChatRequest;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.message.SystemMessage;
import com.lyh.base.agent.domain.message.UserMessage;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.skills.model.Skill;
import com.lyh.base.agent.tool.ToolManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/6 23:21
 */
@Slf4j
public class SkillInvoker {
    private ChatModel chatModel;
    private ToolManager toolManager;

    public SkillInvoker(ChatModel chatModel, ToolManager toolManager) {
        this.chatModel = chatModel;
        this.toolManager = toolManager;
    }

    public ChatResponse chat(String query, String skillName) {
        Skill skill = toolManager.getSkill(skillName);
        SystemMessage systemMessage = new SystemMessage(skill.getContent());
        ChatResponse call = chatModel.call(List.of(systemMessage, new UserMessage(query)));
        return call;
    }
}
