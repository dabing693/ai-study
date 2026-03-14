package com.lyh.base.agent.mcp;

import lombok.Data;

import java.util.List;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/14 12:54
 */
@Data
public class ToolsCallResp {
    private Result result;

    @Data
    public static class Result {
        private List<Content> content;

        @Data
        public static class Content{
            private String text;
        }
    }
}
