package com.lyh.finance.tools;

import com.lyh.base.agent.tool.ToolResult;

import java.util.List;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/24 22:31
 */
public class BaseTool {
    public ToolResult getToolResult(String content, List<String> valuableContents) {
        return new ToolResult(content, valuableContents);
    }
}
