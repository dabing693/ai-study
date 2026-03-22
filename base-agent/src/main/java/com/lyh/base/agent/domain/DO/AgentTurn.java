package com.lyh.base.agent.domain.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * agent 轮次上下文
 */
@Data
@TableName("agent_turn")
public class AgentTurn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private LocalDateTime createTime;
}
