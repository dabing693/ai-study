package com.lyh.trade;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;


public class McpClientTest {

    public static void main(String[] args) throws Exception {
        new McpClientTest().run();
    }

    public void run(String... args) throws Exception {
        // 创建客户端
        McpRestTemplateClient client = new McpRestTemplateClient("http://localhost:9080");

        try {
            // 1. 建立连接
            if (!client.connect()) {
                System.err.println("Failed to connect");
                return;
            }
            //client.setSessionId("c403a63f-2335-4b1e-a7e0-2b69adfc03d9");


            // 2. 初始化
            if (!client.initialize()) {
                System.err.println("Failed to initialize");
                return;
            }

            // 3. 获取工具列表
            JsonNode tools = client.listTools();
            System.out.println("\n📋 Available tools:");
            if (tools != null && tools.has("result")) {
                JsonNode toolsArray = tools.get("result").get("tools");
                if (toolsArray != null && toolsArray.isArray()) {
                    for (JsonNode tool : toolsArray) {
                        System.out.println("  - " + tool.get("name").asText() +
                                ": " + tool.get("description").asText());
                    }
                }
            }

            // 4. 调用工具的示例（如果有天气工具）
            JsonNode toolsResult = client.listTools();
            if (toolsResult != null && toolsResult.has("result")) {
                JsonNode toolsArray = toolsResult.get("result").get("tools");
                if (toolsArray != null && toolsArray.isArray()) {
                    for (JsonNode tool : toolsArray) {
                        String toolName = tool.get("name").asText();
                        if (toolName.contains("weather")) {
                            System.out.println("\n🌤️ Calling weather tool...");
                            Map<String, Object> args2 = new HashMap<>();
                            args2.put("city", "Beijing");
                            JsonNode result = client.callTool(toolName, args2);
                            System.out.println("Result: " + result);
                            break;
                        }
                    }
                }
            }

            // 等待一会儿查看结果
            Thread.sleep(2000);

        } finally {
            // 关闭连接
            client.close();
        }
    }
}