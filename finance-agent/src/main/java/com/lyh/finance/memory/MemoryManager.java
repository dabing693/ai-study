package com.lyh.finance.memory;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.finance.context.RequestContext;
import com.lyh.finance.domain.DO.LlmMemory;
import com.lyh.finance.domain.message.*;
import com.lyh.finance.enums.MemoryStrategy;
import com.lyh.finance.enums.MessageType;
import com.lyh.finance.memory.repository.MilvusMemoryRepository;
import com.lyh.finance.memory.repository.MysqlMemoryRepository;
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
        MemoryStrategy strategy = MemoryStrategy.valueOf(memoryProperty.getStrategy());
        MemoryQuery query = new MemoryQuery(RequestContext.getSession(), userMessage,
                memoryProperty.getMaxMessageNum(), memoryProperty.getMinScore());
        //历史消息
        List<Message> hisMessages = new ArrayList<>();
        if (Objects.equals(strategy, MemoryStrategy.sliding_window)) {
            hisMessages = memory2Message(mysqlMemoryRepository.get(query));
        } else if (Objects.equals(strategy, MemoryStrategy.semantic_call)) {
            //向量数据库取ids，mysql根据id取记忆，在转为message
            hisMessages = memory2Message(mysqlMemoryRepository.getByIds(milvusMemoryRepository.get(query)));
        }
        //去除系统提示词和用户提示词
        int maxHisMsg = memoryProperty.getMaxMessageNum() - 2;
        if (hisMessages.size() > maxHisMsg) {
            //todo 避免新消息被删除，导致没写入数据库
            hisMessages = hisMessages.subList(hisMessages.size() - maxHisMsg, hisMessages.size());
        }
        return hisMessages;
    }

    public void saveMemory(List<Message> messages) {
        List<Message> newMessages = messages.stream()
                //非历史消息才保存
                .filter(it -> !it.isHis())
                .collect(Collectors.toList());
        List<LlmMemory> llmMemories = mysqlMemoryRepository.add(RequestContext.getSession(), newMessages);
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
}
