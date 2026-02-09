package com.lyh.finance.service;

import com.lyh.finance.domain.entity.Conversation;
import com.lyh.finance.mapper.ConversationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Service
public class ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    public List<Conversation> getUserConversations(Long userId) {
        return conversationMapper.selectByUserId(userId);
    }

    public Conversation createOrUpdateConversation(String conversationId, Long userId, String title) {
        // 先查询是否已存在
        Conversation existing = conversationMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (existing != null) {
            // 已存在，更新时间
            existing.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(existing);
            return existing;
        }

        // 不存在，创建新记录
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    public Conversation createConversation(String conversationId, Long userId, String title) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    public Conversation getConversation(String conversationId, Long userId) {
        return conversationMapper.selectByConversationIdAndUserId(conversationId, userId);
    }

    public void updateConversationTitle(String conversationId, Long userId, String title) {
        Conversation conversation = conversationMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    public void deleteConversation(String conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectByConversationIdAndUserId(conversationId, userId);
        if (conversation != null) {
            conversationMapper.deleteById(conversation.getId());
        }
    }
}
