package com.lyh.finance.domain.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lyh.finance.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LlmMemory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private String jsonContent;
}