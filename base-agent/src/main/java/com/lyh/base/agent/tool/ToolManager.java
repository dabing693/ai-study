package com.lyh.base.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.SpringContext;
import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.ToolMessage;
import com.lyh.base.agent.skills.SkillInvoker;
import com.lyh.base.agent.skills.model.Skill;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Slf4j
public class ToolManager {
    private final ToolBuilder toolBuilder;
    private volatile static ToolInvoker toolInvoker;
    private volatile static SkillInvoker skillInvoker;

    public ToolManager(ToolBuilder toolBuilder) {
        this.toolBuilder = toolBuilder;
    }

    public List<FunctionTool> getTools() {
        return toolBuilder.getTools();
    }

    public ToolInvoker toolInvoker() {
        if (toolInvoker == null) {
            synchronized (ToolManager.class) {
                if (toolInvoker == null) {
                    toolInvoker = SpringContext.getBean(ToolInvoker.class);
                }
            }
        }
        return toolInvoker;
    }

    public SkillInvoker skillInvoker() {
        if (skillInvoker == null) {
            synchronized (ToolManager.class) {
                if (skillInvoker == null) {
                    skillInvoker = SpringContext.getBean(SkillInvoker.class);
                }
            }
        }
        return skillInvoker;
    }

    public ToolMessage invoke(String query, AssistantMessage.ToolCall toolCall) {
        AssistantMessage.ToolCall.Function function = toolCall.getFunction();
        log.info("\n调用工具：{}，参数：{}", function.getName(), function.getArguments());
        ToolBuilder.ToolCallBack toolCallBack = toolBuilder.getToolCallBacks()
                .get(function.getName());
        if (toolCallBack == null && toolBuilder.containsSkill(function.getName())) {
            ChatResponse chat = skillInvoker().chat(query, function.getName());
            ToolMessage toolMessage = new ToolMessage(chat.getReply(), null);
            return toolMessage;
        }
        return toolInvoker().invoke(toolCall, toolCallBack);
    }

    public Skill getSkill(String skill) {
        return toolBuilder.getSkill(skill);
    }
}
