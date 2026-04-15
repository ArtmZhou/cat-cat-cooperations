# 聊天室记忆系统分析文档

## 概述

Cat Agent Platform 的聊天记忆系统负责让 Agent 在多轮对话中保持上下文连贯性。系统通过两种互补的机制实现记忆能力：

1. **CLI Session 恢复机制** —— 利用 CLI 工具原生的 `--resume` 功能恢复完整对话上下文
2. **消息持久化机制** —— 将聊天消息存储到 JSON 文件，用于前端展示和群聊上下文注入

---

## 一、记忆系统架构

### 1.1 核心组件

| 组件 | 文件 | 职责 |
|------|------|------|
| `LocalCliSessionService` | `service/LocalCliSessionService.java` | 管理 CLI 会话，解析 sessionId，构建 `--resume` 命令 |
| `LocalCliProcessService` | `service/LocalCliProcessService.java` | 管理进程生命周期，Agent 启停和状态重置 |
| `LocalChatGroupService` | `service/LocalChatGroupService.java` | 群聊消息管理，构建群聊上下文 prompt |
| `JsonFileStore` | `store/JsonFileStore.java` | 通用 JSON 文件持久化存储 |
| `StoredCliAgent` | `store/entity/StoredCliAgent.java` | Agent 实体，包含 `sessionId` 字段 |
| `StoredCliAgentOutputLog` | `store/entity/StoredCliAgentOutputLog.java` | 输出日志实体 |
| `StoredChatGroupMessage` | `store/entity/StoredChatGroupMessage.java` | 群聊消息实体 |

### 1.2 数据存储文件

```
./data/
├── cli_agents.json              # Agent 实例（含 sessionId）
├── cli_agent_output_logs.json   # Agent 输出日志（每 Agent 最多保留 100 条）
├── chat_groups.json             # 群聊定义
└── chat_group_messages.json     # 群聊消息（每群最多 200 条）
```

---

## 二、单聊记忆机制（CLI Agent 直聊）

### 2.1 Session ID 的获取与保存

Agent 的多轮对话记忆依赖于 CLI 工具（如 Claude Code）提供的 **Session ID**。

**工作流程：**

```
用户输入 → sendInput(agentId, input)
         → 构建 CLI 命令（buildPerRequestCommand）
         → 执行子进程（ProcessBuilder）
         → 解析 stream-json 输出
         → 从 "system" 或 "result" 事件中提取 session_id
         → 保存到 StoredCliAgent.sessionId
         → 下次请求时使用 --resume {sessionId}
```

**关键代码（LocalCliSessionService.java）：**

```java
// 构建命令时添加 --resume 参数
if (agent.getSessionId() != null && !agent.getSessionId().isEmpty()) {
    command.add("--resume");
    command.add(agent.getSessionId());
}

// 从 stream-json 的 "system" 事件中捕获 session_id
case "system":
    String sessionId = node.path("session_id").asText(null);
    if (sessionId != null && agent.getSessionId() == null) {
        agent.setSessionId(sessionId);
        cliAgentStore.save(agentId, agent);
    }
    break;

// 从 "result" 事件中更新 session_id
case "result":
    String sessionId = node.path("session_id").asText(null);
    if (sessionId != null) {
        agent.setSessionId(sessionId);
        cliAgentStore.save(agentId, agent);
    }
    break;
```

### 2.2 执行模式

系统使用 **Per-Request 模式**（`--print`）：
- 每次用户输入都会创建一个新的子进程
- 子进程执行完毕后退出
- 不维护持久化的进程连接
- 通过 `--resume {sessionId}` 恢复之前的对话上下文

**命令格式示例：**
```bash
claude --print --output-format stream-json --verbose --resume abc123def456 "用户输入的内容"
```

### 2.3 输出日志持久化

每次 Agent 的输出都会被保存为 `StoredCliAgentOutputLog`：

```java
private void saveOutputLog(String agentId, String type, String content) {
    StoredCliAgentOutputLog outputLog = new StoredCliAgentOutputLog();
    outputLog.setAgentId(agentId);
    outputLog.setType(type);      // "output", "text", "error"
    outputLog.setContent(content);
    outputLog.setTimestamp(LocalDateTime.now());
    outputLogStore.save(id, outputLog);
    cleanupOldOutputLogs(agentId); // 保持每 Agent 最多 100 条
}
```

前端通过 `GET /api/v1/cli-agents/{agentId}/logs?limit=100` 加载历史日志。

---

## 三、群聊记忆机制

### 3.1 消息持久化

群聊中的每条消息（用户、Agent、系统）都存储为 `StoredChatGroupMessage`：

```java
StoredChatGroupMessage {
    String id;
    String groupId;
    String senderType;          // "user", "agent", "system"
    String senderAgentId;
    String senderName;
    String content;
    List<String> mentionedAgentIds;
    boolean broadcast;
    String createdAt;
}
```

### 3.2 上下文注入

当用户在群聊中发送消息时，系统会为每个目标 Agent 构建包含群聊历史的 prompt：

```java
private String buildAgentPrompt(String groupId, String content, String currentAgentId) {
    // 1. 获取最近 20 条群聊消息
    // 2. 构建群聊上下文 prompt
    // 3. 标注当前 Agent 自己的历史消息为 "你(AgentName)"
}
```

**生成的 prompt 示例：**
```
[群聊上下文] 你正在参与群聊「技术讨论」，你的身份是「Claude-1」。
以下是最近的群聊记录，请结合上下文理解并回复最新消息：
---
用户(张三): 帮我分析一下这个架构
Claude-2: 这个架构使用了微服务模式...
你(Claude-1): 我补充一下关于安全性的考虑...
---
最新消息 - 用户: 那性能优化方面呢？
请以「Claude-1」的身份回复。
```

### 3.3 实时输出追踪

群聊中 Agent 的流式输出通过内存 Map 追踪：

```java
// 追踪 agent 当前所在的群聊上下文
private final Map<String, String> agentGroupContext = new ConcurrentHashMap<>();
// 追踪每个 agent 的累积输出文本
private final Map<String, StringBuilder> agentGroupOutputBuffers = new ConcurrentHashMap<>();
```

输出流程：
1. Agent 开始执行 → 注册到 `agentGroupContext`
2. `text_delta` 事件 → 追加到 `agentGroupOutputBuffers`，实时推送到 WebSocket
3. `done` 事件 → 保存完整输出为 `StoredChatGroupMessage`，清理内存状态

### 3.4 消息限制

- 每群最多保留 **200 条**消息（`MAX_MESSAGES_PER_GROUP = 200`）
- 上下文注入最多使用最近 **20 条**消息
- 超出限制的旧消息会被自动删除

---

## 四、Agent 重启后的记忆恢复

### 4.1 Agent 手动停止/重启

**优化前的行为（存在问题）：**

当 Agent 被手动停止时，`stopProcess()` 会清除 `sessionId`：

```java
// LocalCliProcessService.stopProcess()
agent.setSessionId(null);  // 丢弃对话上下文！
```

这意味着手动停止再启动后，Agent 无法使用 `--resume` 恢复之前的对话。

**优化后的行为：**

保留 `sessionId`，让 Agent 在重启后能恢复对话上下文。`sessionId` 只在用户主动清空日志时才清除。

### 4.2 后端服务重启

**优化前的行为（存在问题）：**

后端服务重启时，`@PostConstruct` 方法会重置所有运行中的 Agent：

```java
@PostConstruct
public void resetAgentStatusOnStartup() {
    // 将所有 RUNNING/EXECUTING 的 Agent 重置为 STOPPED
    agent.setStatus("STOPPED");
    agent.setProcessId(null);
    agent.setSessionId(null);  // 丢弃对话上下文！
}
```

**优化后的行为：**

保留 `sessionId`，只重置进程状态。这样 Agent 重新启动后，仍可通过 `--resume` 恢复对话。

### 4.3 各层记忆恢复情况

| 记忆类型 | 数据位置 | Agent 停止/重启 | 后端重启 |
|----------|----------|-----------------|----------|
| Session ID | `cli_agents.json` | ✅ 保留（优化后） | ✅ 保留（优化后） |
| 输出日志 | `cli_agent_output_logs.json` | ✅ 保留 | ✅ 保留 |
| 群聊消息 | `chat_group_messages.json` | ✅ 保留 | ✅ 保留 |
| 群聊上下文 | 内存 Map | ❌ 丢失 | ❌ 丢失 |
| 流式输出缓冲 | 内存 Map | ❌ 丢失 | ❌ 丢失 |

---

## 五、优化方案

### 已实施的优化

#### 优化 1：Agent 停止时保留 sessionId

**问题：** Agent 停止时清除 sessionId，导致重启后无法恢复对话。

**方案：** 在 `stopProcess()` 中不再清除 `sessionId`。仅在用户主动调用"清空日志"时才清除。

**影响：** Agent 停止后重新启动，可以通过 `--resume` 无缝恢复之前的对话上下文。

#### 优化 2：后端重启时保留 sessionId

**问题：** `@PostConstruct` 中的 `resetAgentStatusOnStartup()` 会清除所有 Agent 的 sessionId。

**方案：** 重置 Agent 状态为 STOPPED、清除 processId，但保留 sessionId。

**影响：** 后端服务重启后，Agent 重新启动仍能恢复之前的对话上下文。

#### 优化 3：输出日志保留量可配置化

**问题：** 硬编码的 `MAX_LOG_ENTRIES = 100` 可能不够灵活。

**方案：** 通过 `application.yml` 配置项 `cat.cli.max-output-logs` 控制每 Agent 的最大日志保留量，默认 500 条。

**影响：** 用户可以根据需要调整日志保留量，保留更多历史对话记录。

#### 优化 4：停止 Agent 时提供清除 sessionId 选项

**问题：** 有时用户确实希望清除对话上下文重新开始。

**方案：** 在停止 Agent 的 API 中增加 `clearSession` 参数，默认为 `false`。同时在清空日志时同步清除 sessionId。

---

## 六、技术总结

### 核心记忆链路

```
                   ┌─────────────────────────┐
                   │    CLI 工具 (Claude)     │
                   │   内部维护完整对话历史    │
                   └───────────┬─────────────┘
                               │ session_id
                   ┌───────────▼─────────────┐
                   │   StoredCliAgent.json    │
                   │   持久化 sessionId       │
                   └───────────┬─────────────┘
                               │ --resume {sessionId}
                   ┌───────────▼─────────────┐
                   │  Per-Request 子进程      │
                   │  恢复完整对话上下文       │
                   └───────────┬─────────────┘
                               │ stream-json
              ┌────────────────┼────────────────┐
              ▼                ▼                 ▼
   ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐
   │  输出日志(JSON)  │ │  WebSocket   │ │ 群聊消息(JSON)│
   │  前端历史展示    │ │  实时推送    │ │  上下文注入   │
   └─────────────────┘ └──────────────┘ └──────────────┘
```

### 设计特点

1. **无状态进程模型**：每次请求创建新进程，通过 sessionId 恢复状态，天然支持分布式部署
2. **双层记忆**：CLI 工具维护完整对话（通过 session），平台维护展示日志和群聊上下文
3. **持久化存储**：所有关键数据使用 JSON 文件持久化，重启不丢失
4. **轻量级设计**：不需要数据库，JSON 文件直接可读，便于调试和运维
