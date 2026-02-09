package com.lyh.base.agent.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryQuery {
    private String conversationId;
    private String query;
    private Integer limit;
    /**
     * 最小的相关性分数
     */
    private Double minScore;
}
