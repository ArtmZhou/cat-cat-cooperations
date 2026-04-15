# 聊天功能改进文档

**修复日期:** 2026-04-15
**状态:** 已完成

---

## 1. 概述

本次修改解决了聊天系统的三个核心问题：群聊@提及输入体验、群聊Agent上下文感知、以及聊天室会话记录恢复。

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

## 4. 聊天室会话记录恢复

### 4.1 问题描述

在聊天室（ChatRoom）中与某个Agent聊天后，如果切换到其他页面再切回来，之前的会话记录不会回显，需要重新选择Agent。

### 4.2 修改方案

通过localStorage持久化选中的Agent ID，在页面重新进入时自动恢复选中状态和消息历史。

### 4.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-web/src/views/chat/ChatRoomView.vue` | 新增Agent选择状态持久化逻辑 |

### 4.4 localStorage键值

| Key | 说明 |
|-----|------|
| `cli-agent-chat-history` | 消息历史（已有） |
| `cli-agent-selected-agent-id` | 选中的Agent ID（新增） |

### 4.5 恢复流程

1. `onMounted` → 加载消息历史 → 加载Agent列表 → 从localStorage恢复选中Agent → 加载该Agent的消息历史 → 连接WebSocket并订阅
2. `selectAgent` → 保存Agent ID到localStorage
3. `onUnmounted` → 保存当前消息历史到localStorage

### 4.6 新增方法

| 方法 | 说明 |
|------|------|
| `saveSelectedAgentId(agentId)` | 保存选中的Agent ID到localStorage |
| `restoreSelectedAgent()` | 从localStorage恢复选中的Agent |

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

### 5.3 聊天室会话记录恢复

1. 进入聊天室，选择一个Agent
2. 发送几条消息，等待回复
3. 切换到其他页面（如仪表盘）
4. 再切回聊天室页面
5. 验证之前选中的Agent仍然选中，消息历史正确显示
