package com.lyh.base.agent.memory;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.enums.MemoryStrategy;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 响应式记忆管理器，直接从 Reactor Context 中获取会话信息，避免对 ThreadLocal 的依赖
 * @author Gemini CLI
 */
@Slf4j
@RequiredArgsConstructor
public class ReactiveMemoryManager {
    private final MemoryProperty memoryProperty;
    private final MysqlMemoryRepository mysqlMemoryRepository;
    private final MilvusMemoryRepository milvusMemoryRepository;
    private final ExecutorService milvusThreadPool;

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
            return Mono.fromCallable(() -> {
                MemoryStrategy strategy = MemoryStrategy.valueOf(memoryProperty.getStrategy());
                MemoryQuery query = new MemoryQuery(sessionId, userMessage,
                        memoryProperty.getMaxMessageNum(), memoryProperty.getMinScore());
                
                List<Message> hisMessages = new ArrayList<>();
                if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
                    hisMessages = memory2Message(mysqlMemoryRepository.get(query));
                } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
                    hisMessages = memoryVector2Message(milvusMemoryRepository.get(query));
                }

                int maxHisMsg = memoryProperty.getMaxMessageNum() - 2;
                if (hisMessages.size() > maxHisMsg) {
                    hisMessages = hisMessages.subList(hisMessages.size() - maxHisMsg, hisMessages.size());
                }
                return hisMessages;
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    /**
     * 保存记忆，自动从 Reactor Context 中提取 UserContext
     */
    public Mono<Void> saveMemory(List<Message> newMessages) {
        return Mono.deferContextual(ctx -> {
            RequestContext.UserContext userContext = ctx.get(RequestContext.USER_CONTEXT_KEY);
            if (userContext == null) {
                log.warn("Reactor Context 中未找到 UserContext，无法保存记忆");
                return Mono.empty();
            }
            String sessionId = userContext.getConversationId();

            return Mono.<Void>fromRunnable(() -> {
                List<LlmMemory> llmMemories = mysqlMemoryRepository.add(sessionId, newMessages);
                newMessages.forEach(it -> it.setHis(true));
                // 异步存入向量数据库，复用原来的线程池逻辑
                milvusThreadPool.execute(() -> milvusMemoryRepository.add(sessionId, llmMemories));
            }).subscribeOn(Schedulers.boundedElastic());
        });
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
                msg = JSONObject.parseObject(it.getJsonContent(), com.lyh.base.agent.domain.message.UserMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.assistant)) {
                msg = JSONObject.parseObject(it.getJsonContent(), com.lyh.base.agent.domain.message.AssistantMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.tool)) {
                msg = JSONObject.parseObject(it.getJsonContent(), com.lyh.base.agent.domain.message.ToolMessage.class);
            } else if (Objects.equals(it.getType(), MessageType.system)) {
                msg = JSONObject.parseObject(it.getJsonContent(), com.lyh.base.agent.domain.message.SystemMessage.class);
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
        List<Message> msgList = new ArrayList<>();
        for (LlmMemoryVector it : vectorList) {
            if (it == null || !org.springframework.util.StringUtils.hasText(it.getContent())) {
                continue;
            }
            Message msg = new com.lyh.base.agent.domain.message.UserMessage(it.getContent());
            msg.setHis(true);
            msgList.add(msg);
        }
        return msgList;
    }
}
