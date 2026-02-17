package com.lyh.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.orchestrator.AgentRegistry;
import com.lyh.base.agent.orchestrator.AgentResult;
import com.lyh.base.agent.orchestrator.MultiAgentOrchestrator;
import com.lyh.finance.agents.*;
import com.lyh.finance.interceptor.AuthInterceptor;
import com.lyh.finance.service.ConversationService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 多 Agent 会诊 Controller
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/multi-agent")
public class MultiAgentController {

    @Autowired
    private MultiAgentOrchestrator multiAgentOrchestrator;

    @Autowired
    private ConversationService conversationService;

    private final ObjectMapper objectMapper;

    private static final List<String> PARALLEL_AGENTS = Arrays.asList(
            "技术分析Agent", "基本面分析Agent", "市场情绪Agent"
    );

    private static final List<String> SEQUENTIAL_AGENTS = Arrays.asList(
            "风险评估Agent", "总结报告Agent"
    );
    private final AgentRegistry agentRegistry;
    private final TechnicalAnalysisAgent technicalAnalysisAgent;
    private final FundamentalAnalysisAgent fundamentalAnalysisAgent;
    private final MarketSentimentAgent marketSentimentAgent;
    private final RiskAssessmentAgent riskAssessmentAgent;
    private final SummaryReportAgent summaryReportAgent;

    @PostConstruct
    public void agentRegistry() {
        agentRegistry.register("技术分析Agent", "负责分析股票的技术面：K线、技术指标、趋势等",
                technicalAnalysisAgent, "技术分析", "K线", "技术指标");
        agentRegistry.register("基本面分析Agent", "负责分析股票的基本面：财报、估值、成长性等",
                fundamentalAnalysisAgent, "基本面分析", "财报", "估值");
        agentRegistry.register("市场情绪Agent", "负责分析市场情绪：新闻、舆情、资金流向等",
                marketSentimentAgent, "市场情绪", "新闻", "舆情");
        agentRegistry.register("风险评估Agent", "负责基于前面的分析结果进行风险评估",
                riskAssessmentAgent, "风险评估", "仓位建议");
        agentRegistry.register("总结报告Agent", "负责整合所有分析结果，生成最终报告",
                summaryReportAgent, "总结报告", "投资建议");
    }

    @GetMapping(value = "/consultation/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> consultationStream(
            @RequestParam("query") String query,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestHeader(value = "isNew", required = false) String isNewHeader,
            HttpServletResponse response) {

        response.setCharacterEncoding("UTF-8");
        boolean isNew = "true".equalsIgnoreCase(isNewHeader);

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
            isNew = true;
        }

        Long userId = AuthInterceptor.getCurrentUserId();
        String finalConversationId = conversationId;
        boolean finalIsNew = isNew;

        if (finalIsNew && userId != null) {
            String title = query.length() > 30 ? query.substring(0, 30) + "..." : query;
            conversationService.createOrUpdateConversation(finalConversationId, userId, title);
        }

        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                RequestContext.setSession(finalConversationId, finalIsNew);
                sendEvent(emitter, "session", finalConversationId);
                sendEvent(emitter, "start", "多Agent会诊开始");

                String finalResult = multiAgentOrchestrator.executeConsultation(
                        query,
                        PARALLEL_AGENTS,
                        SEQUENTIAL_AGENTS,
                        agentResult -> {
                            try {
                                sendEvent(emitter, "agent_result", objectMapper.writeValueAsString(agentResult));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

                sendEvent(emitter, "content", finalResult);
                sendEvent(emitter, "done", "会诊完成");
                emitter.complete();
            } catch (Exception ex) {
                safeSend(emitter, "error", ex.getMessage());
                emitter.completeWithError(ex);
            } finally {
                RequestContext.clear();
            }
        });

        return ResponseEntity.ok().body(emitter);
    }

    private void safeSend(SseEmitter emitter, String eventName, String data) {
        try {
            sendEvent(emitter, eventName, data);
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }
}
