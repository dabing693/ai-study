# Milvus 2.5.x 知识点（本次对话整理）

## 字段与 schema
- `content`：原始文本字段。做 BM25 全文检索时，Attu 里选 `BM25(VarChar)`（或 TEXT），并在该字段上建 BM25 索引。
- `content_vector`：密集向量（embedding）字段，用于向量相似度检索。
- `content_embeddings`：稀疏向量字段（SparseFloatVector），由 `Function: BM25(content)` 在服务端从 `content` 自动生成。
- 若使用服务端自动生成 BM25，通常不需要自建 `sparse_vector`，但 `content_embeddings` 字段仍然存在，且由函数生成。

## 混合检索（BM25 + Dense）
- 用 BM25 的文本检索 + 密集向量检索进行融合。
- Java v2 支持 `EmbeddedText`：
  - BM25 请求：`AnnSearchReq` 指向文本字段（如 `content`），`metricType(BM25)`，`vectors(new EmbeddedText(query))`。
  - Dense 请求：`AnnSearchReq` 指向 `content_vector`，`FloatVec` + `COSINE`/`IP`。
  - 用 `HybridSearchReq` + `RRFRanker(new RRFRanker(60))` 做融合排序。
- v1 的 `MilvusServiceClient.hybridSearch()` 使用 v1 参数，不支持 `EmbeddedText`。

## Attu 建表字段选择
- `content`：`BM25(VarChar)`，长度设大（如 1024/4096/65535）。
- `content_vector`：`FloatVector(dim)`。
- `content_embeddings`：`SparseFloatVector`（由 `Function: BM25(content)` 生成）。
- `conversation_id`：`VarChar`，可作为 partition key。

## 写入：v1 vs v2
- v1 写入主要是列式（`InsertParam`），通常要求提供所有非空字段。
- v2 写入主推行式（`InsertReq`，JSON 行）。
- 若 `content_embeddings` 为非空字段，v1 列式写入但未提供该字段会报错：
  - `ParamException: The field: content_embeddings is not provided.`
- 解决：用 v2 行式写入，只提供 `content` + `content_vector`，让服务端 Function 自动生成 `content_embeddings`。

## 迁移思路（老集合 -> 新集合）
- 老集合：`llm_memory_vectors`（无 `content`）。
- 新集合：`finance_agent_memory`（有 `content` + `content_embeddings`）。
- 步骤：
  1) v2 Query 分页从老集合取 `id + content_vector`。
  2) 用 `id` 去 MySQL 查 `content/type/conversationId`。
  3) v2 Insert 行式写入新集合，触发 BM25 Function 自动生成 `content_embeddings`。
- 过滤策略：跳过 `system` / `tool` 类型与空 `content`（与现有记忆策略一致）。

## 注意点
- Milvus Query 的 `offset` 分页不保证稳定顺序，可能漏/重复；可改为按 `id` 范围分页。
- 迁移后数量不一致时，优先检查是否被过滤（如 `type=tool`）。
## 报错分析
- `failed to create query plan: field (content) to search is not of vector data type` 表示 BM25 检索需要的字段 `content` 在集合中不是可检索的 TEXT/BM25 字段，或 Milvus 版本不支持 `EmbeddedText`/BM25。
- 解决：确认 Milvus 2.5.x，并修正 schema/索引；或改为在 `content_embeddings`（SparseFloatVector）上做稀疏向量检索，不要指向 `content` 普通 VarChar。
- 如果用了旧集合（没有 `content`/BM25），请区分 `llm_memory_vectors` 与 `finance_agent_memory` 并切换到新集合。
