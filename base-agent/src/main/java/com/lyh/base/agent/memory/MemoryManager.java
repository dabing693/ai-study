package com.lyh.base.agent.memory;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.RequestContext;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.domain.DO.LlmMemoryVector;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.domain.message.*;
import com.lyh.base.agent.enums.MemoryStrategy;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.repository.MilvusMemoryRepository;
import com.lyh.base.agent.memory.repository.MysqlMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MemoryManager {
    private final MemoryProperty memoryProperty;
    private final MysqlMemoryRepository mysqlMemoryRepository;
    private final MilvusMemoryRepository milvusMemoryRepository;
    private final ExecutorService milvusThreadPool;

    public List<Message> loadMemory(String userMessage) {
        if (RequestContext.isNewConversation()) {
            return Collections.emptyList();
        }
        MemoryStrategy strategy = MemoryStrategy.valueOf(memoryProperty.getStrategy());
        MemoryQuery query = new MemoryQuery(RequestContext.getSession(), userMessage,
                memoryProperty.getMaxMessageNum(), memoryProperty.getMinScore());
        //历史消息
        List<Message> hisMessages = new ArrayList<>();
        if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
            hisMessages = memory2Message(mysqlMemoryRepository.get(query));
        } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
            //向量数据库取ids，mysql根据id取记忆，在转为message
            hisMessages = memoryVector2Message(milvusMemoryRepository.get(query));
        }
        //去除系统提示词和用户提示词
        int maxHisMsg = memoryProperty.getMaxMessageNum() - 2;
        if (hisMessages.size() > maxHisMsg) {
            hisMessages = hisMessages.subList(hisMessages.size() - maxHisMsg, hisMessages.size());
        }
        return hisMessages;
    }

    public void saveMemory(List<Message> newMessages) {
        List<LlmMemory> llmMemories = mysqlMemoryRepository.add(RequestContext.getSession(), newMessages);
        //存完数据库，就变成历史消息了
        newMessages.forEach(it -> it.setHis(true));
        //异步存入向量数据库
        milvusThreadPool.execute(() -> milvusMemoryRepository.add(RequestContext.getSession(), llmMemories));
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
            msg.setHis(true);
            msg.setCreate(it.getTimestamp());
            msgList.add(msg);
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
            //todo 值得注意 这里的构造的记忆消息 没第一次创建时间 目前在逻辑上倒没影响
            msgList.add(msg);
        }
        return msgList;
    }
}
