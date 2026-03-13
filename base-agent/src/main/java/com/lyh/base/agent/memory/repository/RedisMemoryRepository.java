package com.lyh.base.agent.memory.repository;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.domain.message.*;
import com.lyh.base.agent.enums.MessageType;
import com.lyh.base.agent.memory.MemoryProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RedisMemoryRepository {
    private final RedisTemplate<String, String> redisTemplate;
    private final MemoryProperty memoryProperty;

    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String key = buildKey(conversationId);
        int maxSize = memoryProperty.getActiveWindow();
        try {
            List<String> strMessages = messages.stream()
                    //非系统提示词才存redis，与mysql get时不查询系统提示词保持一致
                    .filter(it -> !Objects.equals(it.getRole(), MessageType.system.name()))
                    .map(it -> serializeMessage(it))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(strMessages)) {
                return;
            }
            //push到右边
            Long totalLen = redisTemplate.opsForList().rightPushAll(key, strMessages);
            log.info("Redis缓存消息成功, conversationId={}, count={}", conversationId, strMessages.size());
            if (totalLen > maxSize) {
                //删除左边
                redisTemplate.opsForList().trim(key, -maxSize, -1);
                log.info("Redis删除消息, conversationId={}, delNum={}", conversationId, totalLen - maxSize);
            }
            redisTemplate.expire(key, memoryProperty.getRedisKeyExpireMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis缓存消息失败, conversationId={}", conversationId, e);
        }
    }

    public List<Message> get(String conversationId, int limit) {
        String key = buildKey(conversationId);
        try {
            List<String> jsonList = redisTemplate.opsForList().range(key, 0, limit - 1);
            if (jsonList == null || jsonList.isEmpty()) {
                return Collections.emptyList();
            }
            List<Message> messages = new ArrayList<>();
            for (String json : jsonList) {
                Message msg = deserializeMessage(json);
                if (msg != null) {
                    msg.setHis(true);
                    messages.add(msg);
                }
            }
            return messages;
        } catch (Exception e) {
            log.error("Redis读取缓存失败, conversationId={}", conversationId, e);
            return Collections.emptyList();
        }
    }

    public void clear(String conversationId) {
        String key = buildKey(conversationId);
        try {
            redisTemplate.delete(key);
            log.debug("Redis缓存已清除, conversationId={}", conversationId);
        } catch (Exception e) {
            log.error("Redis清除缓存失败, conversationId={}", conversationId, e);
        }
    }

    private String buildKey(String conversationId) {
        return memoryProperty.getRedisKeyPrefix() + conversationId;
    }

    private String serializeMessage(Message message) {
        try {
            return JSONObject.toJSONString(message);
        } catch (Exception e) {
            log.error("消息序列化失败", e);
            return null;
        }
    }

    private Message deserializeMessage(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            String role = obj.getString("role");
            MessageType msgType = MessageType.valueOf(role);

            return switch (msgType) {
                case user -> obj.toJavaObject(UserMessage.class);
                case assistant -> obj.toJavaObject(AssistantMessage.class);
                case tool -> obj.toJavaObject(ToolMessage.class);
                case system -> obj.toJavaObject(SystemMessage.class);
            };
        } catch (Exception e) {
            log.error("消息反序列化失败, json={}", json, e);
            return null;
        }
    }
}
