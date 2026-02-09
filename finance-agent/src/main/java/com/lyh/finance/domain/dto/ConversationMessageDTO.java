package com.lyh.finance.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Data
public class ConversationMessageDTO {
    private Long id;
    private String conversationId;
    private String content;
    private String type;
    private LocalDateTime timestamp;
}
