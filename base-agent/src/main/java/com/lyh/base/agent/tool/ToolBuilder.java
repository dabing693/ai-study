package com.lyh.base.agent.tool;

import com.lyh.base.agent.annotation.Tool;
import com.lyh.base.agent.annotation.ToolParam;
import com.lyh.base.agent.domain.FunctionTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
public class ToolBuilder {
    private final List<Object> toolObjects;
    //对应模型工具调用的参数
    private final List<FunctionTool> tools;
    //存放实际工具调用时的元数据
    private final ConcurrentHashMap<String, ToolCallBack> toolCallBacks = new ConcurrentHashMap<>();

    public ToolBuilder(Object... toolObjects) {
        this.toolObjects = Arrays.asList(toolObjects);
        this.tools = initTools();
    }

    public ToolManager buildToolManager() {
        return new ToolManager(this);
    }

    public List<FunctionTool> getTools() {
        return tools;
    }

    public ConcurrentHashMap<String, ToolCallBack> getToolCallBacks() {
        return toolCallBacks;
    }

    private List<FunctionTool> initTools() {
        List<FunctionTool> tools = new ArrayList<>();
        for (Object obj : toolObjects) {
            tools.addAll(buildFromObject(obj));
        }
        return tools;
    }

    private List<FunctionTool> buildFromObject(Object obj) {
        List<FunctionTool> tools = new ArrayList<>();
        Method[] methods = obj.getClass().getMethods();
        for (Method method : methods) {
            FunctionTool modelTool = buildFromMethod(obj, method);
            if (modelTool != null) {
                tools.add(modelTool);
            }
        }
        return tools;
    }

    private FunctionTool buildFromMethod(Object object, Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return null;
        }
        FunctionTool.Function function = new FunctionTool.Function();
        //工具名称 英文 未配置时用方法名
        function.setName(StringUtils.hasLength(tool.name()) ? tool.name() : method.getName());
        //工具描述
        function.setDescription(tool.description());
        //工具参数
        function.setParameters(buildParams(method));
        add2ToolCallBack(function, method, object);
        return new FunctionTool(function);
    }

    private FunctionTool.Function.Parameters buildParams(Method method) {
        Parameter[] methodParameters = method.getParameters();
        //工具的所有属性 保持顺序
        LinkedHashMap<String, FunctionTool.Function.Parameters.Property> properties = new LinkedHashMap<>();
        //工具的必须属性，未配置@ToolParam时是必须
        List<String> required = new ArrayList<>();
        String[] parameterNames = new StandardReflectionParameterNameDiscoverer()
                .getParameterNames(method);
        for (int i = 0; i < methodParameters.length; i++) {
            Parameter param = methodParameters[i];
            FunctionTool.Function.Parameters.Property property = new FunctionTool.Function.Parameters.Property();
            property.setClz(param.getType());
            property.setType(param.getType().getSimpleName().toLowerCase(Locale.ROOT));
            String paramName = parameterNames[i];
            ToolParam toolParam = param.getAnnotation(ToolParam.class);
            if (toolParam != null) {
                property.setDescription(toolParam.description());
                if (toolParam.required()) {
                    required.add(paramName);
                }
            } else {
                required.add(paramName);
            }
            properties.put(paramName, property);
        }
        return new FunctionTool.Function.Parameters(properties, required);
    }

    public void add2ToolCallBack(FunctionTool.Function function, Method method, Object object) {
        toolCallBacks.put(function.getName(), new ToolCallBack(method, object,
                function.getParameters()));
    }

    @Data
    @AllArgsConstructor
    public static class ToolCallBack {
        private Method method;
        private Object object;
        private FunctionTool.Function.Parameters parameters;
    }
}
