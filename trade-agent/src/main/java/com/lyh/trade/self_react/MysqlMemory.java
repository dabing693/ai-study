package com.lyh.trade.self_react;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.trade.self_react.domain.DO.LlmMemory;
import com.lyh.trade.self_react.domain.DO.LlmMemoryVector;
import com.lyh.trade.self_react.domain.message.*;
import com.lyh.trade.self_react.embedding.GeminiEmbed;
import com.lyh.trade.self_react.enums.MemoryStrategy;
import com.lyh.trade.self_react.enums.MessageType;
import com.lyh.trade.self_react.mapper.LlmMemoryMapper;
import com.lyh.trade.self_react.embedding.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Slf4j
@Component
public class MysqlMemory {
    @Autowired
    private LlmMemoryMapper llmMemoryMapper;
    @Autowired
    private GeminiEmbed geminiEmbed;
    @Autowired
    private VectorSearchService vectorSearchService;
    @Autowired
    private ExecutorService milvusThreadPool;

    public void add(Message message) {
        addAll(Collections.singletonList(message));
    }

    public void addAll(List<Message> messages) {
        List<LlmMemory> list = new ArrayList<>();
        for (Message message : messages) {
            LlmMemory llmMemory = new LlmMemory();
            llmMemory.setConversationId(RequestContext.getSession());
            llmMemory.setContent(message.storedContent());
            llmMemory.setType(MessageType.valueOf(message.getRole()));
            llmMemory.setTimestamp(message.getCreate());
            llmMemory.setJsonContent(JSONObject.toJSONString(message));
            list.add(llmMemory);

        }
        llmMemoryMapper.insert(list);
        //存入向量数据库
        milvusThreadPool.execute(() -> saveToMilvus(list));
    }

    public List<Message> get(String uniqueId, Integer limit,
                             String query,
                             MemoryStrategy strategy) {
        if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
            return selectTable(uniqueId, limit);
        } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
            return searchSimilar(uniqueId, limit, query);
        }
        return Collections.emptyList();
    }

    private List<Message> selectTable(String uniqueId, Integer limit) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", uniqueId);
        //不查系统提示词
        queryWrapper.ne("type", MessageType.system.name());
        //按时间降序排序 优先取最先的
        queryWrapper.orderByDesc("timestamp");
        queryWrapper.last("limit " + limit);

        return toMessage(llmMemoryMapper.selectList(queryWrapper));
    }

    private List<Message> searchSimilar(String uniqueId, Integer limit,
                                        String query) {
        List<Long> ids = vectorSearchService.searchSimilarVectors(uniqueId, query, limit);
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);

        return toMessage(llmMemoryMapper.selectList(queryWrapper));
    }

    private List<Message> toMessage(List<LlmMemory> memoryList) {
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

    private void saveToMilvus(List<LlmMemory> llmMemoryList) {
        List<LlmMemoryVector> vectorList = new ArrayList<>();
        for (LlmMemory it : llmMemoryList) {
            //system tool消息不保存，因为向量搜索的目的是召回相关的历史记忆。
            // 示例1：它今天表现咋样？示例2：你之前提到的xxx，是啥意思？
            // 示例1：得过一层query改写解决
            if (Objects.equals(it.getType(), MessageType.system) ||
                    Objects.equals(it.getType(), MessageType.tool)) {
                continue;
            }
            LlmMemoryVector vector = new LlmMemoryVector();
            vector.setId(it.getId().longValue());
            vector.setType(it.getType().name());
            vector.setConversation_id(it.getConversationId());
            vector.setContent_vector(geminiEmbed.genVector(it.getContent()));
            vectorList.add(vector);
        }
        if (CollectionUtils.isEmpty(vectorList)) {
            return;
        }
        vectorSearchService.insertBatch(vectorList);
        log.info("成功保存到向量数据库，条数：{}", vectorList.size());
    }
}
