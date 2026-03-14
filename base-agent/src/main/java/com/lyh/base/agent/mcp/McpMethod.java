package com.lyh.base.agent.mcp;

public enum McpMethod {
    TOOLS_LIST("tools/list"),
    TOOLS_CALL("tools/call"),
    RESOURCES_LIST("resources/list"),
    NOTIFICATIONS_INITIALIZED("notifications/initialized"),
    INITIALIZE("initialize"),
    ;
    private String name;

    private McpMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
