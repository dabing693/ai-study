package com.lyh.finance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private Long userId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
