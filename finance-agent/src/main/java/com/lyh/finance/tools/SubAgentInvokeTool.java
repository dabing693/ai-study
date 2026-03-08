package com.lyh.finance.tools;

import com.lyh.base.agent.annotation.Tool;
import com.lyh.base.agent.annotation.ToolParam;
import com.lyh.base.agent.orchestrator.AgentResult;
import com.lyh.base.agent.orchestrator.MultiAgentOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A2A 通信协议工具
 * 允许 Coordinator Agent 通过大模型的原生 Tool Calling 协议调度领域专家 Agent
 */
@Slf4j
@Component
public class SubAgentInvokeTool {

    private final MultiAgentOrchestrator multiAgentOrchestrator;

    public SubAgentInvokeTool(@Lazy MultiAgentOrchestrator multiAgentOrchestrator) {
        this.multiAgentOrchestrator = multiAgentOrchestrator;
    }

    @Tool(name = "invoke_sub_agent", description = "调用领域专家Agent（如：技术分析Agent、基本面分析Agent、市场情绪Agent、风险评估Agent、总结报告Agent）以获取特定领域的专业分析或答案。请勿用于未注册的Agent。")
    public String invoke(
            @ToolParam(description = "需要调用的目标 Agent 名称，例如：'技术分析Agent'") String agentName,
            @ToolParam(description = "下发给子 Agent 的具体任务指令、问题或上下文") String query) {

        log.info("Coordinator 调用子 Agent: {} 任务: {}", agentName, query);

        try {
            MultiAgentOrchestrator.OrchestrationContext ctx = MultiAgentOrchestrator.OrchestrationContextHolder.getContext();
            if (ctx != null) {
                String agentConvId = UUID.randomUUID().toString().replace("-", "");
                if (ctx.getMappingService() != null) {
                    ctx.getMappingService().createMapping(ctx.getParentConversationId(), agentName, agentConvId, "由总指挥动态调度");
                }
                AgentResult result = multiAgentOrchestrator.executeSingleAgent(
                        agentName, query, ctx.getEventConsumer(), agentConvId, ctx.getParentConversationId(), ctx.getMappingService());
                return result.getContent();
            } else {
                AgentResult result = multiAgentOrchestrator.executeSingleAgent(agentName, query, null, UUID.randomUUID().toString(), null, null);
                return result.getContent();
            }
        } catch (Exception e) {
            log.error("调用子 Agent {} 失败!", agentName, e);
            return "调用失败：" + e.getMessage();
        }
    }
}
