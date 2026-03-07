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
import java.util.ArrayList;

import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.memory.MemoryManager;
import com.lyh.base.agent.define.ReActAgent;
import com.lyh.base.agent.domain.FunctionTool;

import java.util.stream.Collectors;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/6 23:21
 */
@Slf4j
public class SkillInvoker {
    private ChatModel chatModel;
    private ToolManager toolManager;
    private MemoryManager memoryManager;

    public SkillInvoker(ChatModel chatModel, ToolManager toolManager, MemoryManager memoryManager) {
        this.chatModel = chatModel;
        this.toolManager = toolManager;
        this.memoryManager = memoryManager;
    }

    public ChatResponse chat(String query, String skillName) {
        Skill skill = toolManager.getSkill(skillName);
        SystemMessage systemMessage = systemMessage(skill);

        ReActAgent subAgent = new ReActAgent(chatModel, memoryManager, toolManager) {
            @Override
            public SystemMessage systemMessage() {
                return systemMessage;
            }

            @Override
            public ChatResponse plan(List<Message> messageList) {
                List<FunctionTool> tools = toolManager.getTools().stream()
                        .filter(t -> !skillName.equals(t.getFunction().getName()))
                        .collect(Collectors.toList());
                return chatModel.call(messageList, tools);
            }

            @Override
            protected void addAndSave(List<Message> messages,
                                      List<Message> newMessages) {
                if (newMessages != null && !newMessages.isEmpty()) {
                    messages.addAll(newMessages);
                }
            }

            @Override
            public List<Message> sense(String query) {
                List<Message> messageList = new ArrayList<>();
                addAndSave(messageList, systemMessage());
                addAndSave(messageList, new UserMessage(query));
                return messageList;
            }
        };

        return subAgent.chat(query);
    }

    private static final String system_prompt_prefix = """
            # Role
            You are very skilled at using the following skills, and you need to accurately use the skills below to solve user problems.
            
            """;

    private SystemMessage systemMessage(Skill skill) {
        return new SystemMessage(system_prompt_prefix + skill.getContent());
    }
}
