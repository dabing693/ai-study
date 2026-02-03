package com.lyh.trade.self_react;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lyh.trade.self_react.domain.DO.LlmMemory;
import com.lyh.trade.self_react.domain.message.*;
import com.lyh.trade.self_react.enums.MessageType;
import com.lyh.trade.self_react.mapper.LlmMemoryMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Component
public class MysqlMemory {
    @Resource
    private LlmMemoryMapper llmMemoryMapper;

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
    }

    public List<Message> get(String uniqueId, Integer limit) {
        QueryWrapper<LlmMemory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", uniqueId);
        //按时间降序排序 优先取最先的
        queryWrapper.orderByDesc("timestamp");
        queryWrapper.last("limit " + limit);
        List<LlmMemory> llmMemories = llmMemoryMapper.selectList(queryWrapper)
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
            msgList.add(msg);
        }
        return msgList;
    }
}
