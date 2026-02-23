package com.lyh.base.agent.orchestrator;

import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.define.BaseAgent;
import com.lyh.base.agent.define.StreamableAgent;
import com.lyh.base.agent.domain.StreamEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class MultiAgentOrchestrator {

    private final AgentRegistry agentRegistry;
    private final ExecutorService executorService;

    public MultiAgentOrchestrator(AgentRegistry agentRegistry, ExecutorService executorService) {
        this.agentRegistry = agentRegistry;
        this.executorService = executorService;
    }

    public String executeConsultation(String query,
                                      List<String> parallelAgents,
                                      List<String> sequentialAgents,
                                      Consumer<AgentEvent> eventConsumer) {
        return executeConsultation(null, query, parallelAgents, sequentialAgents, eventConsumer, null);
    }

    public String executeConsultation(String parentConversationId,
                                      String query,
                                      List<String> parallelAgents,
                                      List<String> sequentialAgents,
                                      Consumer<AgentEvent> eventConsumer,
                                      AgentConversationMappingService mappingService) {
        List<AgentResult> allResults = new ArrayList<>();
        Map<String, String> agentConversationIds = new HashMap<>();

        List<String> allAgents = new ArrayList<>();
        allAgents.addAll(parallelAgents);
        allAgents.addAll(sequentialAgents);

        log.info("开始多 Agent 会诊，并行 Agents: {}, 顺序 Agents: {}", parallelAgents, sequentialAgents);

        for (String agentName : allAgents) {
            String agentConvId = UUID.randomUUID().toString().replace("-", "");
            agentConversationIds.put(agentName, agentConvId);

            if (mappingService != null && parentConversationId != null) {
                String description = agentRegistry.getAgentDescription(agentName);
                mappingService.createMapping(parentConversationId, agentName, agentConvId, description);
            }
        }

        List<AgentResult> parallelResults = executeParallel(
                parallelAgents, query, eventConsumer, agentConversationIds, parentConversationId, mappingService);
        allResults.addAll(parallelResults);

        String context = buildContext(query, parallelResults);
        List<AgentResult> sequentialResults = executeSequential(
                sequentialAgents, context, allResults, eventConsumer, agentConversationIds, parentConversationId, mappingService);
        allResults.addAll(sequentialResults);

        if (!sequentialResults.isEmpty()) {
            return sequentialResults.get(sequentialResults.size() - 1).getContent();
        } else if (!parallelResults.isEmpty()) {
            return parallelResults.get(0).getContent();
        }
        return "未生成结果";
    }

    private List<AgentResult> executeParallel(List<String> agentNames, String query,
                                              Consumer<AgentEvent> eventConsumer,
                                              Map<String, String> agentConversationIds,
                                              String parentConversationId,
                                              AgentConversationMappingService mappingService) {
        List<Future<AgentResult>> futures = new ArrayList<>();

        for (String agentName : agentNames) {
            String agentConvId = agentConversationIds.get(agentName);
            Future<AgentResult> future = executorService.submit(() -> {
                return executeSingleAgent(agentName, query, eventConsumer, agentConvId,
                        parentConversationId, mappingService);
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

    private List<AgentResult> executeSequential(List<String> agentNames, String initialContext,
                                                List<AgentResult> previousResults,
                                                Consumer<AgentEvent> eventConsumer,
                                                Map<String, String> agentConversationIds,
                                                String parentConversationId,
                                                AgentConversationMappingService mappingService) {
        List<AgentResult> results = new ArrayList<>();
        String currentContext = initialContext;

        for (String agentName : agentNames) {
            String agentConvId = agentConversationIds.get(agentName);
            AgentResult result = executeSingleAgent(agentName, currentContext, eventConsumer, agentConvId,
                    parentConversationId, mappingService);
            results.add(result);
            previousResults.add(result);

            if ("success".equals(result.getStatus())) {
                currentContext = buildContext(currentContext, results);
            }
        }
        return results;
    }

    private AgentResult executeSingleAgent(String agentName, String input,
                                           Consumer<AgentEvent> eventConsumer,
                                           String agentConversationId,
                                           String parentConversationId,
                                           AgentConversationMappingService mappingService) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行 Agent: {}", agentName);

        String description = agentRegistry.getAgentDescription(agentName);

        if (mappingService != null && parentConversationId != null) {
            mappingService.updateAgentStatus(parentConversationId, agentName, "running", startTime, null);
        }

        if (eventConsumer != null) {
            eventConsumer.accept(AgentEvent.start(agentName, agentConversationId, description));
        }

        try {
            BaseAgent agent = agentRegistry.getAgent(agentName);

            if (agent == null) {
                throw new RuntimeException("Agent 不存在: " + agentName);
            }

            String originalConversationId = RequestContext.getSession();
            RequestContext.setSession(agentConversationId, true);

            StringBuilder contentBuilder = new StringBuilder();

            try {
                if (agent instanceof StreamableAgent) {
                    ((StreamableAgent) agent).chatStream(input, streamEvent -> {
                        if (eventConsumer != null) {
                            AgentEvent agentEvent = convertStreamEvent(agentName, streamEvent);
                            if (agentEvent != null) {
                                eventConsumer.accept(agentEvent);
                            }
                        }
                        if ("delta".equals(streamEvent.getType()) && streamEvent.getContent() != null) {
                            contentBuilder.append(streamEvent.getContent());
                        }
                    });
                } else {
                    String result = agent.send(input);
                    contentBuilder.append(result);
                    if (eventConsumer != null) {
                        eventConsumer.accept(AgentEvent.delta(agentName, result));
                    }
                }
            } finally {
                if (originalConversationId != null) {
                    RequestContext.setSession(originalConversationId, false);
                } else {
                    RequestContext.clear();
                }
            }

            LocalDateTime endTime = LocalDateTime.now();
            String content = contentBuilder.toString();
            AgentResult agentResult = AgentResult.success(agentName, description, content, startTime, endTime);

            log.info("Agent 执行完成: {}, 耗时: {}ms", agentName, agentResult.getDurationMs());

            if (mappingService != null && parentConversationId != null) {
                mappingService.updateAgentStatus(parentConversationId, agentName, "success", startTime, endTime);
            }

            if (eventConsumer != null) {
                eventConsumer.accept(AgentEvent.done(agentName, "success", content));
            }

            return agentResult;
        } catch (Exception e) {
            log.error("Agent 执行失败: {}", agentName, e);
            LocalDateTime endTime = LocalDateTime.now();
            AgentResult agentResult = AgentResult.error(agentName, description, e.getMessage(), startTime, endTime);

            if (mappingService != null && parentConversationId != null) {
                mappingService.updateAgentStatus(parentConversationId, agentName, "error", startTime, endTime);
            }

            if (eventConsumer != null) {
                eventConsumer.accept(AgentEvent.error(agentName, e.getMessage()));
            }

            return agentResult;
        }
    }

    private AgentEvent convertStreamEvent(String agentName, StreamEvent streamEvent) {
        if (streamEvent == null) return null;

        String type = streamEvent.getType();
        if (type == null) return null;

        switch (type) {
            case "delta":
                return AgentEvent.delta(agentName, streamEvent.getContent());
            case "reasoning_delta":
                return AgentEvent.reasoning(agentName, streamEvent.getReasoningContent());
            case "tool_call":
                return AgentEvent.toolCall(agentName, streamEvent.getToolCalls());
            case "tool_result":
                return AgentEvent.toolResult(agentName, streamEvent.getContent());
            default:
                return null;
        }
    }

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

    public interface AgentConversationMappingService {
        void createMapping(String parentConversationId, String agentName, String agentConversationId, String description);
        void updateAgentStatus(String parentConversationId, String agentName, String status, LocalDateTime startTime, LocalDateTime endTime);
    }
}
