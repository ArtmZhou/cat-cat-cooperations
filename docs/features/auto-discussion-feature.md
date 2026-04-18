# Agent 自动讨论（博弈）功能

**实现日期:** 2026-04-16
**状态:** 已完成

---

## 1. 概述

在群聊中新增"自动讨论"模式，开启后 Agent 通过 @mention 机制互相讨论和博弈。用户发出一个话题后，Agent 会自动回复并在输出中 @其他 Agent 来触发对方回应，形成链式讨论。支持 3 个、4 个甚至更多 Agent 的群聊。

### 核心能力

- **@驱动的链式讨论**：Agent 只有被 @mention 时才会回应，讨论路由完全由 Agent 自主决定
- **多 Agent 支持**：支持 3+ Agent 群聊，Agent 可以 @一个或多个其他成员
- **自然结束**：当 Agent 认为讨论充分或达成共识，不 @任何人，讨论链自然结束
- **轮数控制**：可配置最大讨论轮数（默认6轮），防止无限循环
- **用户中断**：用户可随时点击"中断讨论"按钮停止自动讨论
- **用户介入**：用户发送新消息会自动重置讨论轮数，开启新一轮讨论
- **@mention 解析**：自动从 Agent 输出中解析 @AgentName 模式并路由

---

## 2. 架构设计

### 2.1 自动讨论流程（@驱动）

```
用户发送消息 → 触发群内所有 Agent 回复（第0轮，Prompt含@指令）
    ↓
Agent1 回复并 @Agent3 → 系统解析@mention → 只触发 Agent3（第1轮）
    ↓
Agent3 回复并 @Agent1 @Agent2 → 系统解析 → 触发 Agent1 和 Agent2（第2轮）
    ↓
Agent1 回复并 @Agent2 → 系统解析 → 只触发 Agent2（第3轮）
    ↓
Agent2 回复，不@任何人 → 讨论链自然结束
    ↓
系统消息："Agent2 没有@其他成员，讨论暂停"

中途：
- 用户发送新消息 → 重置轮数，重新开始
- 用户点击"中断讨论" → 立即停止链式触发
- Agent 执行出错 → 该 Agent 跳过
- 达到最大轮数 → 讨论强制结束
```

### 2.2 @mention 解析规则

| 规则 | 说明 |
|------|------|
| 匹配模式 | Agent输出中包含 `@AgentName` |
| 作用域 | 只匹配群组内的其他成员名称 |
| 自我排除 | Agent @自己 会被忽略 |
| 多人 @mention | 支持同时 @多个成员，被@的都会回应 |
| 无@mention | 讨论链自然结束，不自动轮流 |

### 2.3 安全机制

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

### 7.1 初始回复 Prompt（用户消息触发）

当群组开启自动讨论时，发送给 Agent 的 Prompt 中包含 @mention 指令：

```
[群聊上下文] 你正在参与群聊「{群组名}」，你的身份是「{Agent名}」。
群聊成员：你({Agent名})、AgentB、AgentC
以下是最近的群聊记录...
---
最新消息 - 用户: {消息内容}
请以「{Agent名}」的身份回复。
【重要】如果你想让其他成员回应你，请在回复中使用 @成员名称。
只有被你@的成员才会回应。可用的成员：@AgentB、@AgentC
```

### 7.2 自动讨论 Prompt（Agent间链式触发）

```
[群聊讨论模式] 你正在参与群聊「{群组名}」的讨论，你的身份是「{Agent名}」。
这是一场多Agent讨论/辩论，你需要认真思考并回应其他参与者的观点。

群聊成员：你({Agent名})、AgentA、AgentC

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
5. 【重要】你必须在回复中使用 @成员名称 来指定你希望谁来回应你。
   只有被你@的成员才会看到并回应你的消息。
   可用的成员：@AgentA、@AgentC
   你可以@一个或多个成员。
   如果你认为讨论已经充分或达成共识，可以不@任何人，讨论将自然结束。
```

### 7.3 @mention 解析

Agent 输出完成后，系统通过 `parseAgentMentions()` 方法扫描输出内容中的 `@AgentName` 模式：
- 将 Agent 名称映射回 Agent ID
- 排除发送者自己
- 支持同时@多个成员
- 解析结果保存到消息的 `mentionedAgentIds` 字段
- 解析结果用于路由下一轮自动讨论

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
🔄 自动讨论进行中... 等待被@的Agent回应
```

### 8.3 @mention 标签

Agent 消息中如果@了其他成员，消息头部会显示 @mention 标签（与用户消息的 @标签一致）。

### 8.4 群组创建/编辑对话框

新增"自动讨论"设置区域：
- 开启/关闭开关
- 最大轮数配置
- 功能说明：「Agent通过@指定成员来互相回应和讨论，只有被@的Agent才会响应」

---

## 9. 测试验证

### 9.1 基本流程（@驱动）

1. 创建包含 3+ 个 RUNNING 状态 Agent 的群聊
2. 开启自动讨论模式
3. 发送一条话题消息
4. 观察 Agent 回复中是否包含 @其他Agent
5. 验证只有被@的 Agent 响应
6. 验证 Agent 不@任何人时讨论自然结束

### 9.2 多人@mention

1. 观察 Agent 同时@两个其他成员
2. 验证两个被@的成员都会收到消息并回复

### 9.3 用户中断

1. 在自动讨论进行中，点击"中断讨论"按钮
2. 验证讨论立即停止
3. 验证系统消息"🛑 用户中断了自动讨论"

### 9.4 用户介入

1. 在自动讨论进行中，发送新消息
2. 验证旧的讨论链中断
3. 验证新的讨论链开始

### 9.5 后端测试

```bash
# 运行所有测试
mvn test -pl cat-standalone -am

# 运行推送隔离测试
mvn test -pl cat-standalone -Dtest=LocalCliOutputPushServiceTest
```
