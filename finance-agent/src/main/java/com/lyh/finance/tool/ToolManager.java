package com.lyh.finance.tool;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.finance.domain.FunctionTool;
import com.lyh.finance.domain.message.AssistantMessage;
import com.lyh.finance.domain.message.ToolMessage;
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

    public ToolManager(ToolBuilder toolBuilder) {
        this.toolBuilder = toolBuilder;
    }

    public List<FunctionTool> getTools() {
        return toolBuilder.getTools();
    }

    public ToolMessage invoke(AssistantMessage.ToolCall toolCall) {
        AssistantMessage.ToolCall.Function function = toolCall.getFunction();
        log.info("\n调用工具：{}，参数：{}", function.getName(), function.getArguments());
        ToolBuilder.ToolCallBack toolCallBack = toolBuilder.getToolCallBacks()
                .get(function.getName());
        if (toolCallBack == null) {
            return null;
        }
        //{"query":"贵州茅台最近3天利好消息"}
        JSONObject arguments = JSONObject.parse(function.getArguments());
        //模型返回的各个参数的值
        Map<String, Object> valueMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            //todo 模型返回参数值的类型和实际的参数类型对齐
            Object value = entry.getValue();
            valueMap.put(key, value);
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
        Object toolResult = invoke(toolCallBack, params);
        log.info("\n工具调用结果：\n{}", toolResult);
        return new ToolMessage(ToolMessage.strContent(toolResult), toolCall.getId());
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
}
