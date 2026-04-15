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

## 5. 聊天室状态隔离修复

### 5.1 问题描述

聊天室存在两个关键问题：
1. **对话内容未正确显示**: 发送消息后，助手的响应在某些情况下未能正确更新到消息列表中。错误处理时仅修改了 `messages.value` 中的对象属性，但未同步更新 `messageHistory`，导致状态不一致。
2. **跨Agent状态污染**: 与agent1对话时，agent2的对话页面也显示「发送请求...」状态。原因是 `isLoading` 和 `cliStatus` 是全局单一状态，不区分Agent。

### 5.2 修改方案

将 `isLoading` 和 `cliStatus` 从全局单一状态改为按Agent ID存储的状态Map，通过 `computed` 属性自动读取当前选中Agent的状态。同时修复错误处理逻辑，使用 `updateAssistantMessage` 统一更新消息历史。

### 5.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-web/src/views/chat/ChatRoomView.vue` | 状态隔离重构、错误处理修复 |

### 5.4 状态变量变更

| 旧变量 | 新变量 | 说明 |
|--------|--------|------|
| `isLoading: ref(false)` | `loadingStates: ref<Record<string, boolean>>({})` | 按agentId存储加载状态 |
| `cliStatus: ref(null)` | `cliStatusStates: ref<Record<string, ...>>({})` | 按agentId存储CLI状态 |
| - | `isLoading: computed()` | 自动读取当前选中Agent的加载状态 |
| - | `cliStatus: computed()` | 自动读取当前选中Agent的CLI状态 |

### 5.5 关键修改点

1. **WebSocket事件处理**: `done`、`error`、`text_delta`、`output` 事件中的状态更新改为按agentId写入 `loadingStates` 和 `cliStatusStates`，不再依赖 `isSelected` 判断
2. **sendMessage错误处理**: 使用 `updateAssistantMessage()` 替代直接修改 `messages.value` 中的对象属性，确保错误信息同时写入 `messageHistory`
3. **selectAgent**: 切换Agent时根据目标Agent的 `loadingStates` 决定是否显示spinner
4. **模板无需修改**: `isLoading` 和 `cliStatus` 在模板中的引用不变，因为computed属性保持了相同的名称

---

## 6. 聊天室多Agent并行对话与状态增强

### 6.1 问题描述

聊天室存在三个问题：
1. **多Agent并行对话消息丢失**: 同时与2个Agent对话时，切换Agent后旧Agent的WebSocket订阅被取消，导致该Agent的响应内容丢失无法显示。
2. **执行中无状态提示**: 发送请求后显示"发送请求..."，但当Agent进入EXECUTING状态后，聊天页面中没有显示"执行中..."状态提示，用户无法感知Agent正在处理。
3. **缺少耗时显示**: 用户无法知道请求已经执行了多长时间。

### 6.2 修改方案

1. **保持所有Agent订阅**: 切换Agent时不再取消旧Agent的WebSocket订阅，使用`subscribedAgentIds`集合跟踪已订阅的Agent，避免重复订阅。
2. **EXECUTING状态映射**: 在状态变化WebSocket回调中，当Agent状态变为`EXECUTING`时，同步更新`cliStatusStates`显示"执行中..."。
3. **请求耗时计时器**: 新增按Agent独立计时的已用时间显示，从发送请求开始到`done`/`error`事件结束。

### 6.3 修改文件

| 文件 | 修改内容 |
|------|----------|
| `cat-web/src/views/chat/ChatRoomView.vue` | WebSocket订阅管理重构、执行状态显示、耗时计时器 |

### 6.4 新增状态变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `subscribedAgentIds` | `ref<Set<string>>` | 已订阅WebSocket的Agent ID集合 |
| `requestStartTimes` | `ref<Record<string, number>>` | 按agentId存储请求开始时间戳 |
| `elapsedTimes` | `ref<Record<string, string>>` | 按agentId存储已格式化的耗时字符串 |
| `elapsedTime` | `computed<string>` | 当前选中Agent的耗时显示 |

### 6.5 新增方法

| 方法 | 说明 |
|------|------|
| `formatElapsed(ms)` | 将毫秒格式化为"X秒"或"X分Y秒" |
| `startElapsedTimer(agentId)` | 记录请求开始时间，启动全局计时器 |
| `stopElapsedTimer(agentId)` | 清除该Agent的计时，无Agent计时时停止全局计时器 |
| `ensureElapsedTimerRunning()` | 确保全局1秒定时器运行（更新所有Agent的elapsedTimes） |

### 6.6 关键修改点

1. **WebSocket订阅不再取消**: `watch(selectedAgent)` 中移除 `cliWebSocket.unsubscribe(oldAgent.id)` 调用。`subscribeToAgent`内部通过`subscribedAgentIds`检查避免重复订阅。`onUnmounted`统一取消所有已订阅Agent。
2. **EXECUTING状态显示**: `subscribeStatus`回调中，当`data.status === 'EXECUTING'`且Agent处于loading状态时，设置`cliStatusStates[agentId] = { action: '执行中...', detail: 'Agent正在处理请求' }`。
3. **耗时计时器**: `sendMessage`调用`startElapsedTimer(agentId)`。`done`和`error`事件调用`stopElapsedTimer(agentId)`。全局`setInterval`每秒更新所有正在计时的Agent的`elapsedTimes`。
4. **流式接收状态**: `updateAssistantMessage`在收到内容且Agent仍在loading状态时，将状态更新为"接收响应中..."而不是清除状态栏，保持耗时计时器可见。
5. **模板变更**: 状态栏增加 `v-if="elapsedTime"` 的耗时显示区域 `⏱️ {{ elapsedTime }}`。

---

## 7. 测试验证

### 7.1 群聊@提及输入

1. 进入群聊页面，选择一个群组
2. 在输入框中输入`@`，验证弹出Agent选择弹窗
3. 继续输入Agent名称的部分字符，验证列表实时筛选
4. 使用上下箭头键导航，按Enter选择
5. 验证选中的Agent出现在mentioned标签中
6. 验证原有@按钮仍可正常使用

### 7.2 群聊Agent上下文感知

1. 创建包含2+个Agent的群聊
2. 发送一条广播消息
3. 等待Agent1回复
4. 再发送一条消息
5. 验证Agent2的回复中体现了对Agent1回复的感知

### 7.3 聊天室会话记录恢复

1. 进入聊天室，选择一个Agent
2. 发送几条消息，等待回复
3. 切换到其他页面（如仪表盘）
4. 再切回聊天室页面
5. 验证之前选中的Agent仍然选中，消息历史正确显示

### 7.4 聊天室状态隔离

1. 进入聊天室，选择Agent1，发送消息
2. 等待Agent1开始处理（显示"发送请求..."或spinner）
3. 切换到Agent2
4. 验证Agent2的页面没有显示"发送请求..."或spinner
5. Agent2可以正常输入和发送消息
6. 切换回Agent1，验证Agent1的处理状态正确显示
7. 等待Agent1完成，验证状态正确清除

### 7.5 聊天室错误处理

1. 向未运行的Agent发送消息（如果界面允许）
2. 验证错误信息正确显示在消息列表中
3. 验证错误信息被保存到消息历史中
4. 切换到其他页面再切回来，验证错误信息仍然显示

### 7.6 聊天室多Agent并行对话

1. 进入聊天室，选择Agent1，发送一条消息
2. 在Agent1处理期间，立即切换到Agent2
3. 向Agent2也发送一条消息
4. 切换回Agent1
5. 验证Agent1的对话内容已正确显示（响应未丢失）
6. 切换到Agent2，验证Agent2的对话内容也正确显示
7. 验证两个Agent的消息历史互不干扰

### 7.7 聊天室执行状态提示

1. 选择一个运行中的Agent
2. 发送一条消息
3. 验证状态栏先显示"发送请求..."
4. 验证API返回后状态栏变为"执行中..."
5. 验证开始接收输出后状态栏变为"接收响应中..."
6. 验证响应完成后状态栏消失

### 7.8 聊天室耗时显示

1. 选择一个运行中的Agent
2. 发送一条消息
3. 验证状态栏右侧显示"⏱️ X秒"的计时器
4. 验证计时器每秒更新
5. 超过60秒时验证显示格式变为"X分Y秒"
6. 验证响应完成后计时器消失
7. 在Agent处理期间切换到其他Agent再切回来，验证计时器仍在正确计时
