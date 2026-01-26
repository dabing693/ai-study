package com.lyh.trade;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * @author lengYinHui
 * @date 2026/1/23
 */
public class MvcMcpClientTest {
    public static void main(String[] args) {
        start();
    }

    private static void start() {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder("http://localhost:9080").build();
        McpSyncClient client = McpClient.sync(transport).build();
        client.initialize();
        client.ping();
        // 列出并展示可用的工具
        McpSchema.ListToolsResult toolsList = client.listTools();
        System.out.println("可用工具 = " + toolsList);

        // 获取成都的天气
        McpSchema.CallToolResult weatherForecastResult = client.callTool(new McpSchema.CallToolRequest("getWeather",
                Map.of("cityName", "成都")));
        System.out.println("返回结果: " + weatherForecastResult.content());

        client.closeGracefully();
    }
}
