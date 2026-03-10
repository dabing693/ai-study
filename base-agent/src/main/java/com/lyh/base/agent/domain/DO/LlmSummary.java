package com.lyh.base.agent.domain.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("llm_summary")
public class LlmSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String content;
    private LocalDateTime timestamp;
    /**
     * 压缩截至的消息ID
     */
    private Long lastMemoryId;
}
