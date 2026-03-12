# Base-Agent 记忆模块优化方案

## 1. 核心思考（Thought）

*   **目标**：提升记忆检索精确度（Precision）、延长对话上下文（Long-term Context）、降低 Token 成本。
*   **原则**：遵循 KISS 原则（简洁至上），确保与现有 Reactive 异步架构兼容，不引入过度复杂的依赖。
*   **核心逻辑**：将目前的单一检索/滑动窗口模式升级为**“层次化记忆管理 + 混合检索器 + 渐进式压缩”**。

## 2. 现状分析（Current State）

通过阅读代码，发现 `base-agent` 目前的记忆处理逻辑较为基础：
*   **策略选择**：仅支持 `sliding_window`（滑动窗口）和 `semantic_call`（向量语义检索）二选一，缺乏融合。
*   **数据流**：[saveMemory](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/MemoryManager.java#55-62) 仅是将原始消息存入 MySQL 和异步存入 Milvus。
*   **检索深度**：语义检索检索到的结果直接转为 UserMessage，且缺乏对检索结果权重的考量。
*   **压缩缺失**：没有任何自动摘要或压缩机制，长对话后期即使有 RAG，也极易导致上下文窗口溢出。

## 3. 业务调研与方案参考（Research & References）

### 3.1 检索策略
*   **Hybrid Search (RRF)**：参考 [Mem0](https://mem0.ai) 和 [Zep](https://www.getzep.com/) 的做法，结合 **BM25 (关键词)** 和 **Dense Vector (语义)**，利用 RRF (Reciprocal Rank Fusion) 算法合并结果，避免向量搜索在特定关键词（如错别字、专有名词）上的失效。
*   **Tiered Memory Architecture**：借鉴 [MemGPT/Letta](https://letta.com/) 的设计，将记忆分为：
    *   **Core Memory (L1)**：显式的系统提示词与极少数最强关联条目。
    *   **Archival Memory (L2)**：全量向量存储。
    *   **Summary Memory (L3)**：历史摘要。

### 3.2 压缩策略
*   **Progressive Summarization**：参考 [LangChain](https://python.langchain.com/docs/modules/memory/types/summary_buffer) 的 `ConversationSummaryBufferMemory`，当消息达到阈值时，自动调用 LLM 进行摘要提取，而非简单丢弃。
*   **Token Pruning**：根据重要性分数（由 LLM 打分或基于距离）移除低相关性的中间 Tool 调用过程。

## 4. 优化方案设计（Optimized Design）

### 4.1 层次化检索模型 (Layered Retrieval)
建议引入一种新的 `hybrid_tiered` 策略：
1.  **最近上下文 (Sliding Window)**：保持最近 $N$ 条（如 5 条）完整对话。
2.  **语义召回 (Semantic Recall)**：基于 Query 从向量库召回 $K$ 条最相关的历史片段。
3.  **动态摘要 (Conversation Summary)**：加载一个全局摘要，包含之前的历史核心点。

### 4.2 记忆压缩流程
在 [saveMemory](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/MemoryManager.java#55-62) 阶段引入异步任务：
*   **检测点**：当前会话消息数 > 20。
*   **执行任务**：提取前 10 条消息 -> LLM 提取关键事实 (Entity/Intent) -> 更新 `Summary` 表 -> 标记原始消息为 `compressed`。
*   **优点**：大幅减少发送给 LLM 的总 Token 数量。

### 4.3 检索逻辑优化 (RRF Mixed)
修改 [MilvusMemoryRepository](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/repository/MilvusMemoryRepository.java#32-186) 以支持混合权重：
```java
// 伪代码示例
AnnSearchReq sparseReq = ... // BM25
AnnSearchReq denseReq = ... // Cosine
HybridSearchReq req = HybridSearchReq.builder()
    .searchRequests(Arrays.asList(sparseReq, denseReq))
    .ranker(new WeightedRanker(0.6f, 0.4f)) // 赋予关键词更高权重
    .limit(limit)
    .build();
```

## 5. 任务分解清单 (Task List)

| 任务 ID | 任务描述 | 关键点 | 优先级 |
| :--- | :--- | :--- | :--- |
| **M-1** | **扩展配置项** | [MemoryProperty](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/MemoryProperty.java#13-24) 新增 `hybrid_tiered` 策略及 `summarizeThreshold` 等参数。 | 高 |
| **M-2** | **摘要存储层** | 在 [MysqlMemoryRepository](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/repository/MysqlMemoryRepository.java#21-69) 新增 `conversation_summary` 表，存储会话级摘要。 | 中 |
| **M-3** | **混合检索实现** | 优化 `MilvusMemoryRepository.get`，实现 BM25 + Vector 混合检索。 | 高 |
| **M-4** | **异步压缩逻辑** | 在 [MemoryManager](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/MemoryManager.java#24-107) 中实现基于 Token/数量阈值的自动摘要提取器。 | 中 |
| **M-5** | **消息权重计算** | 在 [loadMemory](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/ReactiveMemoryManager.java#33-64) 时，针对 `semantic_call` 返回的结果添加权重标识，并在 Prompt 中区分“相关记忆”与“当前对话”。 | 中 |

## 6. 实现计划 (Implementation Plan)

1.  **第一步**：升级 [MemoryProperty](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/MemoryProperty.java#13-24) 和数据库结构，支持存储摘要。
2.  **第二步**：优化 [MilvusMemoryRepository](file:///d:/Study/git/ai-study/base-agent/src/main/java/com/lyh/base/agent/memory/repository/MilvusMemoryRepository.java#32-186) 为混合检索，提升首轮召回率。
3.  **第三步**：在 `ReActAgent.sense` 逻辑中加入摘要注入，验证摘要对模型判断的影响。
4.  **第四步**：引入异步摘要任务，解决长对话瓶颈。

---

> Implementation Plan, Task List and Thought in Chinese
