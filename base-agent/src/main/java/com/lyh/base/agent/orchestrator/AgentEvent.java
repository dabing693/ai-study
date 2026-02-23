package com.lyh.base.agent.orchestrator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEvent {
    private String eventName;
    private String agentName;
    private String agentConversationId;
    private String agentDescription;
    private String content;
    private String reasoningContent;
    private String toolCalls;
    private String toolResult;
    private String status;

    public static AgentEvent start(String agentName, String agentConversationId, String agentDescription) {
        return AgentEvent.builder()
                .eventName("agent_start")
                .agentName(agentName)
                .agentConversationId(agentConversationId)
                .agentDescription(agentDescription)
                .build();
    }

    public static AgentEvent delta(String agentName, String content) {
        return AgentEvent.builder()
                .eventName("agent_delta")
                .agentName(agentName)
                .content(content)
                .build();
    }

    public static AgentEvent reasoning(String agentName, String reasoningContent) {
        return AgentEvent.builder()
                .eventName("agent_reasoning")
                .agentName(agentName)
                .reasoningContent(reasoningContent)
                .build();
    }

    public static AgentEvent toolCall(String agentName, String toolCalls) {
        return AgentEvent.builder()
                .eventName("agent_tool_call")
                .agentName(agentName)
                .toolCalls(toolCalls)
                .build();
    }

    public static AgentEvent toolResult(String agentName, String toolResult) {
        return AgentEvent.builder()
                .eventName("agent_tool_result")
                .agentName(agentName)
                .toolResult(toolResult)
                .build();
    }

    public static AgentEvent done(String agentName, String status, String content) {
        return AgentEvent.builder()
                .eventName("agent_done")
                .agentName(agentName)
                .status(status)
                .content(content)
                .build();
    }

    public static AgentEvent error(String agentName, String errorMessage) {
        return AgentEvent.builder()
                .eventName("agent_error")
                .agentName(agentName)
                .status("error")
                .content(errorMessage)
                .build();
    }
}
