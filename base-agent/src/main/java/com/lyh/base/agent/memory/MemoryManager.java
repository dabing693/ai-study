package com.lyh.base.agent.memory;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.handler.QueryRewriteModelHandler;
import com.lyh.base.agent.handler.SummaryModelHandler;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.domain.DO.LlmSummary;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.*;
import com.lyh.base.agent.enums.MemoryStrategy;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import com.lyh.base.agent.memory.repository.RedisMemoryRepository;
import com.lyh.base.agent.memory.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
@RequiredArgsConstructor
public class MemoryManager {
    private final MemoryProperty memoryProperty;
    private final MysqlMemoryRepository mysqlMemoryRepository;
    private final RedisMemoryRepository redisMemoryRepository;
    private final MilvusMemoryRepository milvusMemoryRepository;
    private final SummaryRepository summaryRepository;
    private final ExecutorService milvusThreadPool;
    private final SummaryModelHandler summaryModelHandler;
    private final QueryRewriteModelHandler queryRewriteModelHandler;

    public List<Message> loadMemory(String userMessage) {
        if (RequestContext.isNewConversation()) {
            return Collections.emptyList();
        }
        MemoryStrategy strategy = MemoryStrategy.valueOf(memoryProperty.getStrategy());
        MemoryQuery query = new MemoryQuery(RequestContext.getSession(), userMessage,
                memoryProperty.getMaxMessageNum(), memoryProperty.getMinScore(), null);

        List<Message> finalMessages = new ArrayList<>();
        // 1. 如果开启了摘要，尝试加载最新摘要作为背景
        Optional<LlmSummary> latestSummaryOpt = Optional.empty();
        if (memoryProperty.isEnableSummary()) {
            latestSummaryOpt = Optional.ofNullable(summaryRepository.getLatest(RequestContext.getSession()));
            latestSummaryOpt.ifPresent(s -> {
                UserMessage msg = new UserMessage("[历史对话摘要]: " + s.getContent());
                msg.setHis(true);
                finalMessages.add(msg);
            });
        }
        Long minId = latestSummaryOpt.map(LlmSummary::getLastMemoryId).orElse(null);
        query.setMinId(minId);

        // 2. 加载历史消息
        List<Message> hisMessages = new ArrayList<>();
        if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
            hisMessages = memory2Message(mysqlMemoryRepository.get(query));
        } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
            //query改写
            String rewrittenQuery = queryRewrite(userMessage, minId);
            query.setRewrittenQuery(rewrittenQuery);
            hisMessages = memoryVector2Message(milvusMemoryRepository.get(query), true);
        } else if (Objects.equals(strategy, MemoryStrategy.hybrid_tiered)) {
            // L1: 窗口消息 - 优先从Redis读取热数据
            List<Message> l1Messages;
            if (memoryProperty.isEnableRedisCache()) {
                l1Messages = l1MessageFromCache(minId);
            } else {
                MemoryQuery windowQuery = new MemoryQuery(query.getConversationId(),
                        memoryProperty.getActiveWindow(), minId);
                List<LlmMemory> windowMemories = mysqlMemoryRepository.get(windowQuery);
                l1Messages = memory2Message(windowMemories);
            }

            // L2: 语义召回 (Top 5 相关，无视位标，因为向量库全量索引)
            MemoryQuery semanticQuery = new MemoryQuery(query.getConversationId(), userMessage, 5, memoryProperty.getMinScore(), null);
            //query改写
            String rewrittenQuery = queryRewrite(userMessage, minId);
            query.setRewrittenQuery(rewrittenQuery);
            List<LlmMemoryVector> vectors = milvusMemoryRepository.get(semanticQuery);

            // 过滤
            Set<String> l1Contents = l1Messages.stream()
                    .map(Message::storedContent)
                    .collect(Collectors.toSet());
            List<LlmMemoryVector> filteredVectors = vectors.stream()
                    .filter(v -> !l1Contents.contains(v.getContent()))
                    .collect(Collectors.toList());
            List<Message> l2Messages = memoryVector2Message(filteredVectors, true);

            hisMessages.addAll(l2Messages);
            hisMessages.addAll(l1Messages);
        }

        // 3. 去重归并限制条数
        int maxHisMsg = memoryProperty.getMaxMessageNum() - 2;
        if (hisMessages.size() > maxHisMsg) {
            hisMessages = hisMessages.subList(hisMessages.size() - maxHisMsg, hisMessages.size());
        }

        finalMessages.addAll(hisMessages);
        return finalMessages;
    }

    public void saveMemory(List<Message> newMessages) {
        List<LlmMemory> llmMemories = mysqlMemoryRepository.add(RequestContext.getSession(), newMessages);
        // 存完数据库，就变成历史消息了
        newMessages.forEach(it -> it.setHis(true));

        // 存入Redis缓存
        if (memoryProperty.isEnableRedisCache()) {
            redisMemoryRepository.add(RequestContext.getSession(), newMessages);
        }

        // 异步存入向量数据库
        milvusThreadPool.execute(() -> milvusMemoryRepository.add(RequestContext.getSession(), llmMemories));

        // 4. 触发摘要压缩检测
        if (memoryProperty.isEnableSummary()) {
            checkAndCompress(RequestContext.getSession());
        }
    }

    private void checkAndCompress(String sessionId) {
        milvusThreadPool.execute(() -> {
            try {
                // 获取最新位标
                LlmSummary latest = summaryRepository.getLatest(sessionId);
                Long minId = (latest != null) ? latest.getLastMemoryId() : null;

                // 统计位标之后的普通消息数
                long count = mysqlMemoryRepository.countNormalMessages(sessionId, minId);
                if (count >= memoryProperty.getSummaryThreshold()) {
                    log.info("会话 {} 水位线后消息数 {} 触发逻辑摘要压缩", sessionId, count);
                    performCompression(sessionId, latest);
                }
            } catch (Exception e) {
                log.error("逻辑摘要压缩检测失败", e);
            }
        });
    }

    private void performCompression(String sessionId, LlmSummary oldSummary) {
        int activeWindow = memoryProperty.getActiveWindow();
        Long lastId = (oldSummary != null) ? oldSummary.getLastMemoryId() : null;

        // 1. 计算待压缩的消息区间：水位线之后，且保留最近的 activeWindow 条
        long totalAfterWatermark = mysqlMemoryRepository.countNormalMessages(sessionId, lastId);
        int toCompressCount = (int) (totalAfterWatermark - activeWindow);
        if (toCompressCount <= 0) {
            return;
        }
        // 2. 获取最老的 N 条待压缩消息
        List<LlmMemory> memoriesToCompress = mysqlMemoryRepository.getOldestMessages(sessionId, lastId, toCompressCount);
        if (memoriesToCompress.isEmpty()) {
            return;
        }

        // 记录新的水位线 ID
        Long newLastMemoryId = memoriesToCompress.get(memoriesToCompress.size() - 1).getId();
        String intermediateContent = memoriesToCompress.stream()
                .map(it -> it.getType() + ": " + it.getContent())
                .collect(Collectors.joining("\n"));
        String history = oldSummary == null ? null : oldSummary.getContent();
        String newSummaryContent = summaryModelHandler.handle(history, intermediateContent);
        if (StringUtils.hasText(newSummaryContent)) {
            // 4. 保存新位标摘要
            LlmSummary newSummary = new LlmSummary();
            newSummary.setConversationId(sessionId);
            newSummary.setContent(newSummaryContent);
            newSummary.setTimestamp(LocalDateTime.now());
            newSummary.setLastMemoryId(newLastMemoryId);
            summaryRepository.save(newSummary);
            log.info("会话 {} 完成逻辑摘要滚动，位标推进至 {}", sessionId, newLastMemoryId);
        } else {
            log.warn("会话摘要总结为空，session：{}", sessionId);
        }
    }

    private List<Message> memory2Message(List<LlmMemory> memoryList) {
        List<LlmMemory> llmMemories = memoryList
                .stream()
                //按时间进行升序排序，老的放前面
                .sorted(Comparator.comparing(LlmMemory::getTimestamp))
                .collect(Collectors.toList());
        List<Message> msgList = new ArrayList<>();
        for (LlmMemory it : llmMemories) {
            Message msg = null;
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
            msg.setHis(true);
            msg.setCreate(it.getTimestamp());
            msgList.add(msg);
        }
        return msgList;
    }

    private List<Message> memoryVector2Message(List<LlmMemoryVector> vectorList, boolean addWeightTip) {
        if (vectorList == null || vectorList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Message> msgList = new ArrayList<>();
        for (LlmMemoryVector it : vectorList) {
            if (it == null || !StringUtils.hasText(it.getContent())) {
                continue;
            }
            String content = it.getContent();
            if (addWeightTip) {
                content = "[相关历史记忆]: " + content;
            }
            Message msg = new UserMessage(content);
            msg.setHis(true);
            msgList.add(msg);
        }
        return msgList;
    }

    private List<Message> l1MessageFromCache(Long minId) {
        List<Message> l1Messages = redisMemoryRepository.get(RequestContext.getSession(), memoryProperty.getActiveWindow());
        if (l1Messages.size() < memoryProperty.getActiveWindow()) {
            MemoryQuery windowQuery = new MemoryQuery(RequestContext.getSession(), memoryProperty.getActiveWindow(), minId);
            List<LlmMemory> windowMemories = mysqlMemoryRepository.get(windowQuery);
            l1Messages = memory2Message(windowMemories);
            //回填redis
            redisMemoryRepository.add(RequestContext.getSession(), l1Messages);
        }
        return l1Messages;
    }

    private String queryRewrite(String query, Long minId) {
        String resp = null;
        try {
            List<Message> messages = l1MessageFromCache(minId);
            String history = messages.stream().map(it -> it.getRole() + "：" + it.getContent())
                    .collect(Collectors.joining("\n"));
            resp = queryRewriteModelHandler.handle(history, query);
            if (resp == null) {
                return query;
            }
            int start = resp.indexOf('{');
            int end = resp.lastIndexOf('}');
            resp = resp.substring(start, end + 1);
            String newQuery = JSONObject.parseObject(resp).getString("rewritten_query");
            if (!StringUtils.hasText(newQuery)) {
                log.warn("query改写异常，query：{}，响应：{}", query, resp);
                newQuery = query;
            }
            log.info("\n改写前：{}\n改写后：{}", query, newQuery);
            return newQuery;
        } catch (Exception e) {
            log.error("query改写异常，query：{}，resp：{}", query, resp, e);
            return query;
        }
    }
}
