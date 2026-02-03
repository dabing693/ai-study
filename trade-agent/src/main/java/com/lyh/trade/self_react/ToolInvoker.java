package com.lyh.trade.self_react;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.trade.self_react.domain.message.AssistantMessage;
import com.lyh.trade.self_react.domain.message.ToolMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
public class ToolInvoker {
    private static final ConcurrentHashMap<String, ToolCallBack> toolMap = new ConcurrentHashMap<>();

    public static void add(String name, Method method, Object object) {
        toolMap.put(name, new ToolCallBack(method, object));
    }

    public static ToolMessage invoke(AssistantMessage.ToolCall toolCall) {
        AssistantMessage.ToolCall.Function function = toolCall.getFunction();
        String toolName = function.getName();
        log.info("调用工具：{}", toolName);
        ToolCallBack toolCallBack = toolMap.get(toolName);
        if (toolCallBack == null) {
            return null;
        }
        JSONObject arguments = JSONObject.parse(function.getArguments());
        Object[] params = new Object[arguments.size()];
        int ind = 0;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Object value = entry.getValue();
            params[ind++] = value;
        }
        Object toolResult = invoke(toolCallBack, params);
        log.info("工具调用结果：\n{}", toolResult);
        return new ToolMessage(ToolMessage.strContent(toolResult), toolCall.getId());
    }

    public static Object invoke(ToolCallBack toolCallBack, Object... params) {
        try {
            return toolCallBack.getMethod().invoke(toolCallBack.getObject(), params);
        } catch (IllegalAccessException e) {
            log.info("非法获取异常", e);
        } catch (InvocationTargetException e) {
            log.info("执行异常", e);
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    public static class ToolCallBack {
        private Method method;
        private Object object;
    }
}
