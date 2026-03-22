package com.lyh.base.agent.memory;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.domain.DO.LlmSummary;
import com.lyh.base.agent.domain.message.*;
import com.lyh.base.agent.enums.MemoryStrategy;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import com.lyh.base.agent.memory.repository.SummaryRepository;
import com.lyh.base.agent.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 响应式记忆管理器，直接从 Reactor Context 中获取会话信息，避免对 ThreadLocal 的依赖
 *
 * @author Gemini CLI
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveMemoryManager {
    private final MemoryProperty memoryProperty;
    private final MysqlMemoryRepository mysqlMemoryRepository;
    private final MilvusMemoryRepository milvusMemoryRepository;
    private final SummaryRepository summaryRepository;
    private final ExecutorService milvusThreadPool;
    private final ChatModel chatModel;

    /**
     * 加载记忆，自动从 Reactor Context 中提取 UserContext
     */
    public Mono<List<Message>> loadMemory(String userMessage) {
        return Mono.deferContextual(ctx -> {
            RequestContext.UserContext userContext = ctx.getOrDefault(RequestContext.USER_CONTEXT_KEY, null);
            if (userContext == null || Boolean.TRUE.equals(userContext.getNewConversation())) {
                return Mono.just(Collections.emptyList());
            }

            String sessionId = userContext.getConversationId();
            MemoryStrategy strategy = MemoryStrategy.valueOf(memoryProperty.getStrategy());
            MemoryQuery query = new MemoryQuery(sessionId, userMessage,
                    memoryProperty.getMaxMessageNum(), memoryProperty.getMinScore(), null);

            // 1. 加载摘要并确定位标
            Mono<Optional<LlmSummary>> latestSummaryMono = memoryProperty.isEnableSummary() ?
                    Mono.fromCallable(() -> Optional.ofNullable(summaryRepository.getLatest(sessionId)))
                            .subscribeOn(Schedulers.boundedElastic()) :
                    Mono.just(Optional.empty());

            return latestSummaryMono.flatMap(summaryOpt -> {
                Long minTurnId = summaryOpt.map(LlmSummary::getLastTurnId).orElse(null);
                query.setMinId(minTurnId);

                List<Message> summaryMessages = new ArrayList<>();
                summaryOpt.ifPresent(s -> {
                    UserMessage msg =
                            new UserMessage("[历史对话摘要]: " + s.getContent());
                    msg.setHis(true);
                    summaryMessages.add(msg);
                });

                // 2. 加载历史 (L1/L2)
                Mono<List<Message>> historyMono = Mono.fromCallable(() -> {
                    List<Message> hisMessages = new ArrayList<>();
                    if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
                        hisMessages = memory2Message(mysqlMemoryRepository.getRecentTurnMessages(sessionId, minTurnId, memoryProperty.getMaxMessageNum()));
                    } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
                        hisMessages = memoryVector2Message(milvusMemoryRepository.get(query));
                    } else if (Objects.equals(strategy, MemoryStrategy.hybrid_tiered)) {
                        // L1
                        List<LlmMemory> windowMemories = mysqlMemoryRepository.getRecentTurnMessages(sessionId, minTurnId, memoryProperty.getActiveWindow());
                        List<Message> l1 = memory2Message(windowMemories);
                        // L2
                        MemoryQuery semQuery = new MemoryQuery(sessionId, userMessage, 5, memoryProperty.getMinScore(), null);
                        List<LlmMemoryVector> vectors = milvusMemoryRepository.get(semQuery);
                        Set<Long> wIds = windowMemories.stream().map(LlmMemory::getId).collect(Collectors.toSet());
                        List<LlmMemoryVector> filtered = vectors.stream().filter(v -> !wIds.contains(v.getId())).collect(Collectors.toList());
                        List<Message> l2 = memoryVector2Message(filtered);
                        hisMessages.addAll(l2);
                        hisMessages.addAll(l1);
                    }
                    return hisMessages;
                }).subscribeOn(Schedulers.boundedElastic());

                return historyMono.map(his -> {
                    List<Message> combined = new ArrayList<>(summaryMessages);
                    combined.addAll(his);
                    return combined;
                });
            });
        });
    }

    /**
     * 保存记忆，自动从 Reactor Context 中提取 UserContext
     */
    public Mono<Void> saveMemory(List<Message> newMessages) {
        return Mono.deferContextual(ctx -> {
            RequestContext.UserContext userContext = ctx.get(RequestContext.USER_CONTEXT_KEY);
            if (userContext == null) {
                log.warn("Reactor Context 中未找到 UserContext，无法保存 memory");
                return Mono.empty();
            }
            String sessionId = userContext.getConversationId();

            return Mono.<Void>fromRunnable(() -> {
                RequestContext.setUser(userContext);
                List<LlmMemory> llmMemories = mysqlMemoryRepository.add(sessionId, newMessages);
                newMessages.forEach(it -> it.setHis(true));
                // 异步存入向量数据库
                milvusThreadPool.execute(() -> milvusMemoryRepository.add(sessionId, llmMemories));

                // 异步摘要触发检测
                if (memoryProperty.isEnableSummary()) {
                    checkAndCompressAsync(sessionId);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private void checkAndCompressAsync(String sessionId) {
        milvusThreadPool.execute(() -> {
            try {
                LlmSummary latest = summaryRepository.getLatest(sessionId);
                Long minTurnId = (latest != null) ? latest.getLastTurnId() : null;

                long count = mysqlMemoryRepository.countTurns(sessionId, minTurnId);
                if (count >= memoryProperty.getSummaryThreshold()) {
                    log.info("会话 {} 触发响应式逻辑位标摘要压缩", sessionId);
                    performCompression(sessionId, latest);
                }
            } catch (Exception e) {
                log.error("响应式记忆压缩失败", e);
            }
        });
    }

    private void performCompression(String sessionId, LlmSummary oldSummary) {
        int activeWindow = memoryProperty.getActiveWindow();
        Long lastTurnId = (oldSummary != null) ? oldSummary.getLastTurnId() : null;

        long totalTurnsAfterWatermark = mysqlMemoryRepository.countTurns(sessionId, lastTurnId);
        int toCompressTurns = (int) (totalTurnsAfterWatermark - activeWindow);
        if (toCompressTurns <= 0) return;

        Long maxTurnId = mysqlMemoryRepository.getTurnBoundaryIdAsc(sessionId, lastTurnId, toCompressTurns);
        if (maxTurnId == null) return;

        List<LlmMemory> memoriesToCompress = mysqlMemoryRepository.getMessagesByTurnRange(sessionId, lastTurnId, maxTurnId);
        if (memoriesToCompress.isEmpty()) return;

        Long newLastTurnId = memoriesToCompress.get(memoriesToCompress.size() - 1).getTurnId();

        String intermediateContent = memoriesToCompress.stream()
                .map(it -> it.getType() + ": " + it.getContent())
                .collect(Collectors.joining("\n"));

        StringBuilder promptBuilder = new StringBuilder();
        if (oldSummary != null) {
            promptBuilder.append("已知先前的对话摘要：\n").append(oldSummary.getContent()).append("\n\n");
        }
        promptBuilder.append("请结合上述摘要（如有）和以下新增的原始对话片段，生成更新后的综合摘要（300字内）：\n");
        promptBuilder.append(intermediateContent);

        String newSummaryContent = chatModel.call(List.of(new UserMessage(promptBuilder.toString()))).getReply();

        LlmSummary newSummary = new LlmSummary();
        newSummary.setConversationId(sessionId);
        newSummary.setContent(newSummaryContent);
        newSummary.setTimestamp(java.time.LocalDateTime.now());
        newSummary.setLastTurnId(newLastTurnId);
        summaryRepository.save(newSummary);

        log.info("会话 {} [响应式] 逻辑摘要滚动完成，位标推进至 {}", sessionId, newLastTurnId);
    }

    private List<Message> memory2Message(List<LlmMemory> memoryList) {
        List<LlmMemory> llmMemories = memoryList
                .stream()
                .sorted(Comparator.comparing(LlmMemory::getTimestamp))
                .collect(Collectors.toList());
        List<Message> msgList = new ArrayList<>();
        for (LlmMemory it : llmMemories) {
            Message msg;
            if (Objects.equals(it.getType(), MessageType.user)) {
                msg = JSONObject.parseObject(it.getJsonContent(), UserMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.assistant)) {
                msg = JSONObject.parseObject(it.getJsonContent(), AssistantMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.tool)) {
                msg = JSONObject.parseObject(it.getJsonContent(), ToolMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.system)) {
                msg = JSONObject.parseObject(it.getJsonContent(), SystemMessage.class);
            } else {
                throw new RuntimeException("未知消息类型：" + it.getType());
            }
            if (msg != null) {
                msg.setHis(true);
                msg.setCreate(it.getTimestamp());
                msgList.add(msg);
            }
        }
        return msgList;
    }

    private List<Message> memoryVector2Message(List<LlmMemoryVector> vectorList) {
        if (vectorList == null || vectorList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = vectorList.stream().map(LlmMemoryVector::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<LlmMemory> llmMemories = mysqlMemoryRepository.selectByIds(ids);
        return memory2Message(llmMemories);
    }
}
