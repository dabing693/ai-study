# ai-study

用于学习和实验 Spring AI 相关的多模块 Java 项目，涵盖 Agent、工具调用与记忆能力。

## 模块说明

- `ai-common`：通用工具（如 Markdown 处理）。
- `trade-agent`：带工具调用与 MCP 服务的聊天 Agent。
- `finance-agent`：金融方向 Agent，集成 MySQL 与 Milvus 记忆。

其他目录（如 `agent-framework`、`ai-agent-fronted`）为探索性示例，不参与 Maven 聚合构建。

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL（聊天记忆与金融 Agent 数据）
- Milvus（金融 Agent 向量记忆）

## 快速开始

构建全部模块：

```bash
mvn -q -DskipTests package
```

启动 trade-agent：

```bash
mvn -pl trade-agent -am spring-boot:run
```

启动 finance-agent：

```bash
mvn -pl finance-agent -am spring-boot:run
```

## 接口

trade-agent（默认 `server.port=9080`）：

- `GET /chat/generate?query=...`
- `GET /chat/react?query=...`
- `GET /chat/stream?query=...`（SSE）

finance-agent（默认 `server.port=9081`）：

- `GET /react/chat?query=...`

两者都支持可选的 `conversationId` 请求头；未提供时会返回 `X-Session-Id`。

## 配置说明

API Key 与外部服务地址配置在：

- `trade-agent/src/main/resources/application.properties`
- `finance-agent/src/main/resources/application.properties`

不要提交真实密钥，运行前请替换为环境变量或本地配置。

## 备注

- `trade-agent` 使用 Spring AI WebMVC MCP Server。
- `finance-agent` 使用 MySQL + Milvus 作为记忆存储，详见 `md/milvus.md`。

## 启动命令
```shell
//启动aktools
python -m aktools --port 8089
```