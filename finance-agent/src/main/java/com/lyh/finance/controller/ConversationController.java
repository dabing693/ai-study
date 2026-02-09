package com.lyh.finance.controller;

import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.finance.domain.dto.ConversationMessageDTO;
import com.lyh.finance.domain.entity.Conversation;
import com.lyh.finance.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@RestController
@RequestMapping("/api/conversation")
@CrossOrigin(origins = "*")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private LlmMemoryMapper llmMemoryMapper;

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        List<Conversation> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String conversationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        // 验证该对话是否属于当前用户
        Conversation conversation = conversationService.getConversation(conversationId, userId);
        if (conversation == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "对话不存在或无权限");
            return ResponseEntity.status(403).body(error);
        }

        // 查询该对话的所有消息
        List<LlmMemory> memories = llmMemoryMapper.selectByConversationId(conversationId);
        List<ConversationMessageDTO> messages = memories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(messages);
    }

    private ConversationMessageDTO convertToDTO(LlmMemory memory) {
        ConversationMessageDTO dto = new ConversationMessageDTO();
        dto.setId(memory.getId());
        dto.setConversationId(memory.getConversationId());
        dto.setContent(memory.getContent());
        dto.setType(memory.getType() != null ? memory.getType().name().toLowerCase() : null);
        dto.setTimestamp(memory.getTimestamp());
        return dto;
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> delete(@PathVariable String conversationId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未登录");
            return ResponseEntity.status(401).body(error);
        }

        conversationService.deleteConversation(conversationId, userId);
        Map<String, String> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
