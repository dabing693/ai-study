package com.lyh.base.agent.orchestrator;

import com.lyh.base.agent.define.BaseAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 多 Agent 编排器
 * 负责协调多个 Agent 的协作执行
 */
@Slf4j
@RequiredArgsConstructor
public class MultiAgentOrchestrator {

    private final AgentRegistry agentRegistry;
    private final ExecutorService executorService;

    /**
     * 执行多 Agent 会诊（先并行后顺序模式）
     *
     * @param query            用户查询
     * @param parallelAgents   并行执行的 Agent 名称列表
     * @param sequentialAgents 顺序执行的 Agent 名称列表（接收前面所有结果）
     * @param eventConsumer    事件消费者，用于流式输出
     * @return 最终汇总结果
     */
    public String executeConsultation(String query,
                                      List<String> parallelAgents,
                                      List<String> sequentialAgents,
                                      Consumer<AgentResult> eventConsumer) {
        List<AgentResult> allResults = new ArrayList<>();

        log.info("开始多 Agent 会诊，并行 Agents: {}, 顺序 Agents: {}", parallelAgents, sequentialAgents);

        // 1. 并行执行第一阶段 Agents
        List<AgentResult> parallelResults = executeParallel(parallelAgents, query, eventConsumer);
        allResults.addAll(parallelResults);

        // 2. 顺序执行第二阶段 Agents，每个 Agent 接收前面所有结果
        String context = buildContext(query, parallelResults);
        List<AgentResult> sequentialResults = executeSequential(sequentialAgents, context, allResults, eventConsumer);
        allResults.addAll(sequentialResults);

        // 3. 返回最后一个 Agent 的结果（通常是总结 Agent）
        if (!sequentialResults.isEmpty()) {
            return sequentialResults.get(sequentialResults.size() - 1).getContent();
        } else if (!parallelResults.isEmpty()) {
            return parallelResults.get(0).getContent();
        }
        return "未生成结果";
    }

    /**
     * 并行执行多个 Agent
     */
    private List<AgentResult> executeParallel(List<String> agentNames, String query,
                                              Consumer<AgentResult> eventConsumer) {
        List<Future<AgentResult>> futures = new ArrayList<>();

        for (String agentName : agentNames) {
            Future<AgentResult> future = executorService.submit(() -> {
                return executeSingleAgent(agentName, query, eventConsumer);
            });
            futures.add(future);
        }

        List<AgentResult> results = new ArrayList<>();
        for (Future<AgentResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("并行执行 Agent 失败", e);
            }
        }
        return results;
    }

    /**
     * 顺序执行多个 Agent
     */
    private List<AgentResult> executeSequential(List<String> agentNames, String initialContext,
                                                List<AgentResult> previousResults,
                                                Consumer<AgentResult> eventConsumer) {
        List<AgentResult> results = new ArrayList<>();
        String currentContext = initialContext;

        for (String agentName : agentNames) {
            AgentResult result = executeSingleAgent(agentName, currentContext, eventConsumer);
            results.add(result);
            previousResults.add(result);

            if ("success".equals(result.getStatus())) {
                currentContext = buildContext(currentContext, results);
            }
        }
        return results;
    }

    /**
     * 执行单个 Agent
     */
    private AgentResult executeSingleAgent(String agentName, String input,
                                           Consumer<AgentResult> eventConsumer) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行 Agent: {}", agentName);

        try {
            BaseAgent agent = agentRegistry.getAgent(agentName);
            String description = agentRegistry.getAgentDescription(agentName);

            if (agent == null) {
                throw new RuntimeException("Agent 不存在: " + agentName);
            }

            String result = agent.send(input);

            LocalDateTime endTime = LocalDateTime.now();
            AgentResult agentResult = AgentResult.success(agentName, description, result, startTime, endTime);

            log.info("Agent 执行完成: {}, 耗时: {}ms", agentName, agentResult.getDurationMs());

            if (eventConsumer != null) {
                eventConsumer.accept(agentResult);
            }

            return agentResult;
        } catch (Exception e) {
            log.error("Agent 执行失败: {}", agentName, e);
            LocalDateTime endTime = LocalDateTime.now();
            AgentResult agentResult = AgentResult.error(agentName, agentRegistry.getAgentDescription(agentName),
                    e.getMessage(), startTime, endTime);

            if (eventConsumer != null) {
                eventConsumer.accept(agentResult);
            }

            return agentResult;
        }
    }

    /**
     * 构建上下文，将前面的结果整合
     */
    private String buildContext(String originalQuery, List<AgentResult> results) {
        StringBuilder context = new StringBuilder();
        context.append("【原始查询】\n").append(originalQuery).append("\n\n");

        for (AgentResult result : results) {
            context.append("【").append(result.getAgentName()).append("】").append("\n");
            if ("success".equals(result.getStatus())) {
                context.append(result.getContent());
            } else {
                context.append("执行失败: ").append(result.getErrorMessage());
            }
            context.append("\n\n");
        }

        return context.toString();
    }
}
