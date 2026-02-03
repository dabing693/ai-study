package com.lyh.trade.self_react.domain.dao;

import com.lyh.trade.self_react.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LlmMemory {
    private Integer id;
    private String conversationId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
}