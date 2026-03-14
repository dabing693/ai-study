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
public class MemoryQuery {
    private String conversationId;
    private String query;
    private String rewrittenQuery;
    private Integer limit;
    /**
     * 最小的相关性分数
     */
    private Double minScore;
    /**
     * 最小的消息ID限制（逻辑水位线），旧于此ID的消息将被忽略
     */
    private Long minId;

    public MemoryQuery(String conversationId, String query, Integer limit, Double minScore, Long minId) {
        this.conversationId = conversationId;
        this.query = query;
        this.limit = limit;
        this.minScore = minScore;
        this.minId = minId;
    }

    public MemoryQuery(String conversationId, String query, String rewrittenQuery, Double minScore) {
        this.conversationId = conversationId;
        this.query = query;
        this.rewrittenQuery = rewrittenQuery;
        this.minScore = minScore;
    }

    public MemoryQuery(String conversationId, Integer limit, Long minId) {
        this.conversationId = conversationId;
        this.limit = limit;
        this.minId = minId;
    }

    public String getValidQuery() {
        return this.rewrittenQuery != null ? this.rewrittenQuery : this.query;
    }
}
