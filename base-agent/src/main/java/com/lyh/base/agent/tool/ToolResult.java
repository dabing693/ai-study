package com.lyh.base.agent.tool;

import lombok.Data;

import java.util.List;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/24 22:09
 */
@Data
public class ToolResult {
    private String content;
    private List<String> valuableContents;

    public ToolResult(String content, List<String> valuableContents) {
        this.content = content;
        this.valuableContents = valuableContents;
    }

    @Override
    public String toString() {
        return this.content;
    }
}
