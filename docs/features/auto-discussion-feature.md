# Agent 自动讨论（博弈）功能

**实现日期:** 2026-04-16
**状态:** 已完成

---

## 1. 概述

在群聊中新增"自动讨论"模式，开启后 Agent 之间可以自动感知、自动回复，形成多轮博弈/辩论。用户只需发出一个话题，多个 Agent 即会自动展开讨论，用户可随时参与或中断。

### 核心能力

- **自动链式讨论**：一个 Agent 完成回复后，自动触发其他 Agent 进行回应
- **轮数控制**：可配置最大讨论轮数（默认6轮），防止无限循环
- **用户中断**：用户可随时点击"中断讨论"按钮停止自动讨论
- **用户介入**：用户发送新消息会自动重置讨论轮数，开启新一轮讨论
- **讨论质量**：专用的 Prompt 引导 Agent 进行深度讨论，避免简单重复

---

## 2. 架构设计

### 2.1 自动讨论流程

```
用户发送消息 → 触发 Agent1, Agent2 回复（第0轮）
    ↓
Agent1 完成回复 → 等待 Agent2 也完成
    ↓
Agent2 完成回复 → 触发 Agent1 回应 Agent2（第1轮）
    ↓
Agent1 完成回应 → 触发 Agent2 回应 Agent1（第2轮）
    ↓
... 循环直到达到 maxAutoRounds 上限
    ↓
讨论结束，推送系统消息通知

中途：
- 用户发送新消息 → 重置轮数，重新开始
- 用户点击"中断讨论" → 立即停止链式触发
- Agent 执行出错 → 该 Agent 跳过，不影响其他
```

### 2.2 安全机制

| 机制 | 说明 |
|------|------|
| 最大轮数限制 | 默认6轮，可配置1-20轮 |
| 用户中断 | 前端"中断讨论"按钮，立即停止 |
| 用户介入 | 用户发送新消息自动重置讨论 |
| 错误容忍 | 单个 Agent 出错不影响其他 |
| 延迟触发 | 每轮之间有2秒延迟，防止过载 |
| 状态检查 | 只触发 RUNNING 状态的 Agent |

---

## 3. 修改文件

### 3.1 后端

| 文件 | 修改内容 |
|------|----------|
| `StoredChatGroup.java` | 新增 `autoDiscussion` 和 `maxAutoRounds` 字段 |
| `ChatGroupService.java` | 新增 `stopAutoDiscussion`、`isAutoDiscussionRunning`、`updateAutoDiscussionSettings` 接口方法；`ChatGroupInfo` record 增加 `autoDiscussion`、`maxAutoRounds`、`autoDiscussionRunning` 字段 |
| `LocalChatGroupService.java` | 实现自动讨论核心逻辑（链式触发、Prompt构建、轮数管理、状态推送） |
| `ChatGroupController.java` | 新增 `POST /{groupId}/auto-discussion/stop` 和 `GET /{groupId}/auto-discussion/status` 端点；更新 PUT 支持 `autoDiscussion`/`maxAutoRounds` 参数 |

### 3.2 前端

| 文件 | 修改内容 |
|------|----------|
| `chatGroup.js` | 新增 `stopAutoDiscussion` 和 `getAutoDiscussionStatus` API 方法 |
| `websocket.ts` | 新增 `subscribeGroupAutoDiscussionStatus` 订阅方法；更新 `unsubscribeGroup` 清理新订阅 |
| `GroupChatView.vue` | 新增自动讨论开关、轮数配置、中断按钮、状态指示器；更新群组创建/编辑对话框 |

### 3.3 文档

| 文件 | 修改内容 |
|------|----------|
| `CLAUDE.md` | 新增群聊 API 端点和 `LocalChatGroupService` 服务说明 |
| `docs/features/auto-discussion-feature.md` | 本文档 |

---

## 4. API 端点

### 4.1 停止自动讨论

```
POST /api/v1/chat-groups/{groupId}/auto-discussion/stop
```

**响应：** `ApiResponse<Void>` 成功

### 4.2 获取自动讨论状态

```
GET /api/v1/chat-groups/{groupId}/auto-discussion/status
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "running": true
  }
}
```

### 4.3 更新群组（含自动讨论设置）

```
PUT /api/v1/chat-groups/{groupId}
Content-Type: application/json

{
  "name": "讨论群",
  "description": "AI辩论",
  "agentIds": ["agent1", "agent2"],
  "autoDiscussion": true,
  "maxAutoRounds": 8
}
```

---

## 5. WebSocket 主题

| 主题 | 方向 | 说明 |
|------|------|------|
| `/topic/chat-group/{groupId}/auto-discussion-status` | 服务端→客户端 | 自动讨论状态变更通知 |

**消息格式：**
```json
{
  "groupId": "abc123",
  "running": true
}
```

---

## 6. 数据模型变更

### StoredChatGroup 新增字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoDiscussion` | `boolean` | `false` | 是否开启自动讨论模式 |
| `maxAutoRounds` | `int` | `6` | 最大自动讨论轮数 |

---

## 7. 自动讨论 Prompt 设计

与普通群聊 Prompt 不同，自动讨论模式使用专用的 Prompt 模板：

```
[群聊讨论模式] 你正在参与群聊「{群组名}」的讨论，你的身份是「{Agent名}」。
这是一场多Agent讨论/辩论，你需要认真思考并回应其他参与者的观点。

以下是最近的群聊记录：
---
用户(我): 原始话题
AgentA: AgentA的观点...
你(AgentB): 你之前的发言...
---

请针对「AgentA」的最新发言进行回应。
要求：
1. 以「{Agent名}」的身份发表你的观点
2. 你可以赞同、反驳、补充或提出新的角度
3. 保持讨论的深度和质量，避免简单重复他人观点
4. 回复要简洁有力，控制在合理的篇幅内
```

---

## 8. 前端 UI 说明

### 8.1 自动讨论开关

位于群聊头部右侧，包含：
- **开关**: 开启/关闭自动讨论模式
- **轮数输入**: 配置最大讨论轮数（1-20）
- **中断按钮**: 红色按钮，仅在讨论进行中显示，带脉冲动画

### 8.2 讨论状态指示器

当自动讨论进行中但所有 Agent 都未在输出时（轮间等待），显示脉冲指示器：
```
🔄 自动讨论进行中... Agent正在思考下一轮回应
```

### 8.3 群组创建/编辑对话框

新增"自动讨论"设置区域：
- 开启/关闭开关
- 最大轮数配置
- 功能说明文字

---

## 9. 测试验证

### 9.1 基本流程

1. 创建包含 2+ 个 RUNNING 状态 Agent 的群聊
2. 开启自动讨论模式
3. 发送一条话题消息
4. 观察 Agent 自动轮流回复
5. 验证达到最大轮数后自动停止

### 9.2 用户中断

1. 在自动讨论进行中，点击"中断讨论"按钮
2. 验证讨论立即停止
3. 验证系统消息"🛑 用户中断了自动讨论"

### 9.3 用户介入

1. 在自动讨论进行中，发送新消息
2. 验证旧的讨论链中断
3. 验证新的讨论链开始

### 9.4 后端测试

```bash
# 运行所有测试
mvn test -pl cat-standalone -am

# 运行推送隔离测试
mvn test -pl cat-standalone -Dtest=LocalCliOutputPushServiceTest
```
