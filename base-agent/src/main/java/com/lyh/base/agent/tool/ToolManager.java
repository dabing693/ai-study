package com.lyh.base.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.SpringContext;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.ToolMessage;
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

    public ToolMessage invoke(AssistantMessage.ToolCall toolCall) {
        AssistantMessage.ToolCall.Function function = toolCall.getFunction();
        log.info("\n调用工具：{}，参数：{}", function.getName(), function.getArguments());
        ToolBuilder.ToolCallBack toolCallBack = toolBuilder.getToolCallBacks()
                .get(function.getName());
        return toolInvoker().invoke(toolCall, toolCallBack);
    }
}
