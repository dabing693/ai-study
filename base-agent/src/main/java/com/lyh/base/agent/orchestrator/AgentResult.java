package com.lyh.base.agent.orchestrator;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 执行结果封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * Agent 描述
     */
    private String agentDescription;

    /**
     * 执行状态：success, error
     */
    private String status;

    /**
     * 执行结果内容
     */
    private String content;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 执行开始时间
     */
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 执行结束时间
     */
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    public static AgentResult success(String agentName, String agentDescription, String content,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        return AgentResult.builder()
                .agentName(agentName)
                .agentDescription(agentDescription)
                .status("success")
                .content(content)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .build();
    }

    public static AgentResult error(String agentName, String agentDescription, String errorMessage,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        return AgentResult.builder()
                .agentName(agentName)
                .agentDescription(agentDescription)
                .status("error")
                .errorMessage(errorMessage)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .build();
    }
}
