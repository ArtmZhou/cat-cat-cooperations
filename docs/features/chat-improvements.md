# 聊天功能改进文档

**修复日期:** 2026-04-15
**状态:** 已完成

---

## 1. 概述

本次修改解决了群聊系统的核心问题：群聊@提及输入体验、群聊Agent上下文感知、以及群聊推送隔离。

> **注意**: 聊天室（ChatRoom）功能已移除，Agent单聊现在通过CLI Agent详情页直接进行。

---

## 2. 群聊@提及输入优化

### 2.1 问题描述

之前在群聊中指定某个Agent，用户只能通过手动点击输入框左侧的`@`按钮来选择Agent。期望能在输入框中直接输入`@`来触发Agent选择弹窗。

### 2.2 修改方案

在群聊输入框中增加输入监听，当用户输入`@`时自动弹出Agent选择弹窗。

### 2.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-web/src/views/groupChat/GroupChatView.vue` | 新增@触发逻辑、筛选、键盘导航 |

### 2.4 功能特性

- 在输入框中输入`@`后自动弹出Agent选择弹窗
- `@`后继续输入文字可按名称实时筛选Agent列表
- 支持键盘上下箭头（↑↓）导航选项
- 按Enter键或点击选择Agent
- 按Escape键关闭弹窗
- 选择Agent后自动移除输入框中的`@query`文本
- 保留原有的@按钮手动选择方式

### 2.5 新增状态变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `mentionFilter` | `ref<string>` | @后输入的筛选文本 |
| `mentionAtPosition` | `ref<number>` | @符号在输入框中的位置（-1表示非输入触发） |
| `mentionHighlightIndex` | `ref<number>` | 键盘导航当前高亮索引 |
| `filteredMentionAgents` | `computed` | 根据筛选文本过滤后的Agent列表 |

### 2.6 新增方法

| 方法 | 说明 |
|------|------|
| `handleInputChange()` | 监听输入文本变化，检测@触发 |
| `handleInputKeydown(event)` | 处理键盘事件（方向键导航、Enter选择、Escape关闭） |
| `selectMentionFromInput(agentId)` | 从输入@弹窗中选择Agent |
| `closeMentionPopupFromInput()` | 关闭输入触发的@弹窗 |
| `toggleMentionPopupButton()` | @按钮切换弹窗（手动模式） |

---

## 3. 群聊Agent上下文感知

### 3.1 问题描述

在群聊中，Agent之间无法感知到彼此的消息。例如agent1的回复对agent2不可见，导致群聊对话脱节。

### 3.2 修改方案

重构`buildAgentPrompt`方法，在发送给每个Agent的消息中包含群聊的历史上下文，让Agent能理解完整的对话记录。

### 3.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-standalone/src/main/java/com/cat/standalone/service/LocalChatGroupService.java` | 重构`buildAgentPrompt`方法 |

### 3.4 Prompt构建规则

1. **上下文范围**: 最近20条群聊历史消息（排除空内容的占位消息）
2. **身份标注**:
   - 用户消息标注为 `用户(名称)`
   - 当前Agent自己的消息标注为 `你(名称)`
   - 其他Agent消息标注为 `Agent名称`
   - 系统消息标注为 `系统`
3. **Prompt格式**:
   ```
   [群聊上下文] 你正在参与群聊「群组名」，你的身份是「Agent名」。
   以下是最近的群聊记录，请结合上下文理解并回复最新消息：
   ---
   用户(我): 消息内容
   AgentA: 消息内容
   你(AgentB): 消息内容
   ---
   最新消息 - 用户: 最新消息内容
   请以「Agent名」的身份回复。
   ```

### 3.5 方法签名变更

```java
// 旧签名
private String buildAgentPrompt(String content)

// 新签名
private String buildAgentPrompt(String groupId, String content, String currentAgentId)
```

---

## 4. 群聊推送隔离修复

### 4.1 问题描述

群聊中 agent 执行时，后端仍然把同一份 `text_delta / output / error / done` 推送到 `/topic/cli/{agentId}/...`，导致其他已订阅的同名 agent 收到群聊输出。

### 4.2 修改方案

1. **后端按上下文路由 WebSocket**：当 agent 处于群聊上下文时，`LocalCliOutputPushService` 不再向个人 topic 推送 `output / text_delta / error / done / EXECUTING` 事件。
2. **群聊专用收尾处理**：群聊上下文中的 `error` 与 `done` 事件交给 `LocalChatGroupService` 清理上下文和缓存，避免上下文泄漏到下一次对话。
3. **保留最终 RUNNING 状态同步**：群聊任务完成后，仅向 agent 状态 topic 同步一次 `RUNNING`，保证列表状态回正。
4. **群聊前端补 error 分支**：群聊页面收到群聊专属 `error` 输出时，关闭 streaming spinner 并刷新消息列表。

### 4.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-standalone/src/main/java/com/cat/standalone/service/LocalCliOutputPushService.java` | 按群聊上下文隔离推送 |
| `cat-standalone/src/main/java/com/cat/standalone/service/LocalChatGroupService.java` | 暴露群聊上下文判断并补充错误收尾 |
| `cat-web/src/views/groupChat/GroupChatView.vue` | 处理群聊 agent-output 的 `error` 收尾 |
| `cat-standalone/src/test/java/com/cat/standalone/service/LocalCliOutputPushServiceTest.java` | 新增推送隔离单元测试 |

### 4.4 关键修改点

1. **`isAgentInGroupContext(agentId)`**：新增显式上下文判断，供推送层区分不同请求来源。
2. **`pushTextDelta / pushOutput / pushError / pushDone`**：当 agent 正在群聊上下文时，不再写入 `/topic/cli/{agentId}/...`，改为仅走群聊分支。
3. **`pushStatusChange`**：群聊上下文下抑制 `EXECUTING`，仅保留最终 `RUNNING` 状态同步。
4. **`handleAgentError`**：新增群聊错误收尾，清理 `agentGroupContext / agentGroupOutputBuffers`，并追加系统提示消息。
5. **单元测试覆盖**：验证群聊上下文下个人 topic 不再收到 `output / text_delta / error / done / EXECUTING`。

---

## 5. 测试验证

### 5.1 群聊@提及输入

1. 进入群聊页面，选择一个群组
2. 在输入框中输入`@`，验证弹出Agent选择弹窗
3. 继续输入Agent名称的部分字符，验证列表实时筛选
4. 使用上下箭头键导航，按Enter选择
5. 验证选中的Agent出现在mentioned标签中
6. 验证原有@按钮仍可正常使用

### 5.2 群聊Agent上下文感知

1. 创建包含2+个Agent的群聊
2. 发送一条广播消息
3. 等待Agent1回复
4. 再发送一条消息
5. 验证Agent2的回复中体现了对Agent1回复的感知

### 5.3 群聊推送隔离

1. 打开群聊页面，向某个包含 AgentA 的群组发送消息。
2. 验证群聊回复只出现在群聊页面。
3. 运行单元测试 `mvn test -pl cat-standalone -Dtest=LocalCliOutputPushServiceTest`，验证群聊上下文不会把 `output / text_delta / error / done / EXECUTING` 推送到个人 topic。
