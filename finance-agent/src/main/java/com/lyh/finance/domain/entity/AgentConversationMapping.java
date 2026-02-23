package com.lyh.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_conversation_mapping")
public class AgentConversationMapping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String parentConversationId;
    private String agentName;
    private String agentConversationId;
    private String agentDescription;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
