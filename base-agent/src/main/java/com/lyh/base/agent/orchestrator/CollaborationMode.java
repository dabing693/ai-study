package com.lyh.base.agent.orchestrator;

/**
 * Agent 协作模式枚举
 */
public enum CollaborationMode {
    /**
     * 顺序执行：Agent A → Agent B → Agent C
     */
    SEQUENTIAL,

    /**
     * 并行执行：多个 Agent 同时处理
     */
    PARALLEL,

    /**
     * 先并行后顺序：前几个并行，后面顺序执行
     */
    PARALLEL_THEN_SEQUENTIAL
}
