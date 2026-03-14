package com.lyh.base.agent.mcp;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/14 12:35
 */
@Data
public class ToolsListResp {
    private Result result;

    @Data
    public static class Result {
        private List<Tool> tools;

        @Data
        public static class Tool {
            private String name;
            private String description;
            JSONObject inputSchema;
        }
    }
}
