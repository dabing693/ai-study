package com.lyh.finance.controller;

import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.mapper.LlmMemoryMapper;
import com.lyh.finance.domain.dto.ConversationMessageDTO;
import com.lyh.finance.domain.entity.AgentConversationMapping;
import com.lyh.finance.domain.entity.Conversation;
import com.lyh.finance.interceptor.AuthInterceptor;
import com.lyh.finance.service.AgentConversationService;
import com.lyh.finance.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversation")
@CrossOrigin(origins = "*")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private AgentConversationService agentConversationService;

    @Autowired
    private LlmMemoryMapper llmMemoryMapper;

    @GetMapping("/list")
    public ResponseEntity<?> list() {
        Long userId = AuthInterceptor.getCurrentUserId();

        List<Conversation> conversations = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String conversationId) {
        Long userId = AuthInterceptor.getCurrentUserId();

        Conversation conversation = conversationService.getConversation(conversationId, userId);
        if (conversation != null) {
            List<LlmMemory> memories = llmMemoryMapper.selectByConversationId(conversationId);
            List<ConversationMessageDTO> messages = memories.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        }

        AgentConversationMapping mapping = agentConversationService.getMappingByAgentConversationId(conversationId);
        if (mapping != null) {
            Conversation parentConversation = conversationService.getConversation(mapping.getParentConversationId(), userId);
            if (parentConversation != null) {
                List<LlmMemory> memories = llmMemoryMapper.selectByConversationId(conversationId);
                List<ConversationMessageDTO> messages = memories.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(messages);
            }
        }

        Map<String, String> error = new HashMap<>();
        error.put("error", "对话不存在或无权限");
        return ResponseEntity.status(403).body(error);
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
    public ResponseEntity<?> delete(@PathVariable String conversationId) {
        Long userId = AuthInterceptor.getCurrentUserId();

        conversationService.deleteConversation(conversationId, userId);
        Map<String, String> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
