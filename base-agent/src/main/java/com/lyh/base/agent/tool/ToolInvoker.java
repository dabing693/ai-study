package com.lyh.base.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.domain.message.AssistantMessage;
import com.lyh.base.agent.domain.message.ToolMessage;
import com.lyh.base.agent.observation.LangfuseObserver;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/5 14:21
 */
@Slf4j
public class ToolInvoker {
    /**
     * 抽象出ToolInvoker，用来解决标注在ToolManager里面的@LangfuseObserver不生效的问题，因为未交给spring管理
     *
     * @param toolCall
     * @param toolCallBack
     * @return
     */
    @LangfuseObserver
    public ToolMessage invoke(AssistantMessage.ToolCall toolCall,
                              ToolBuilder.ToolCallBack toolCallBack) {
        AssistantMessage.ToolCall.Function function = toolCall.getFunction();
        log.info("\n调用工具：{}，参数：{}", function.getName(), function.getArguments());
        if (toolCallBack == null) {
            return null;
        }
        //{"query":"贵州茅台最近3天利好消息"}
        JSONObject arguments = null;
        if (function.getArguments() != null && !function.getArguments().isBlank()) {
            arguments = JSONObject.parse(function.getArguments());
        }
        //模型返回的各个参数的值
        Map<String, Object> valueMap = new HashMap<>();
        if (arguments != null) {
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String key = entry.getKey();
                //todo 模型返回参数值的类型和实际的参数类型对齐
                Object value = entry.getValue();
                valueMap.put(key, value);
            }
        }
        //真实的方法参数列表
        final LinkedHashMap<String, FunctionTool.Function.Parameters.Property> methodParams =
                toolCallBack.getParameters().getProperties();
        Object[] params = new Object[methodParams.size()];
        int ind = 0;
        List<String> required = toolCallBack.getParameters().getRequired();
        for (Map.Entry<String, FunctionTool.Function.Parameters.Property> entry : methodParams.entrySet()) {
            if (required.contains(entry.getKey()) && !valueMap.containsKey(entry.getKey())) {
                String msg = String.format("必须参数：%s，模型未返回，方法参数列表：%s，模型返回参数：%s",
                        entry.getKey(), methodParams, function.getArguments());
                throw new RuntimeException(msg);
            }
            params[ind++] = valueMap.get(entry.getKey());
        }
        if (toolCallBack.getDynamicCallback() != null) {
            Object toolResult = toolCallBack.getDynamicCallback().apply(valueMap);
            log.info("\n工具调用结果：\n{}", toolResult);
            return buildMessage(toolCall, toolResult);
        }
        Object toolResult = invoke(toolCallBack, params);
        log.info("\n工具调用结果：\n{}", toolResult);
        return buildMessage(toolCall, toolResult);
    }

    public Object invoke(ToolBuilder.ToolCallBack toolCallBack, Object... params) {
        try {
            return toolCallBack.getMethod().invoke(toolCallBack.getObject(), params);
        } catch (IllegalAccessException e) {
            log.error("非法获取异常", e);
        } catch (InvocationTargetException e) {
            log.error("执行异常", e);
        } catch (Exception e) {
            log.error("工具调用异常", e);
        }
        return null;
    }

    private ToolMessage buildMessage(AssistantMessage.ToolCall toolCall, Object toolResult) {
        if (toolResult instanceof ToolResult) {
            ToolResult result = (ToolResult) toolResult;
            return new ToolMessage(result.getContent(), toolCall.getId(), result.getValuableContents());
        } else {
            return new ToolMessage(ToolMessage.strContent(toolResult), toolCall.getId());
        }
    }
}
