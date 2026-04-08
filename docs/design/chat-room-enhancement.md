# 多Agent聊天室增强功能设计文档

## 版本信息

- **版本**: 1.0.0
- **创建日期**: 2026-04-07
- **作者**: Claude Code
- **关联Issue**: #4

---

## 1. 需求概述

### 1.1 背景
当前聊天室仅支持与单个Agent进行1对1交互，无法模拟真实的群组讨论场景。

### 1.2 目标
实现多Agent聊天室功能，支持：
- 多个Agent同时参与同一个对话
- @提及功能，定向指定Agent响应
- 消息广播，所有Agent可见
- 类似真实聊天群的交互体验

### 1.3 业务价值
- **协作场景**: 多个Agent可协作完成复杂任务
- **对比评估**: 同一问题可获得多个Agent的不同观点
- **角色分工**: 不同能力标签的Agent承担不同角色

---

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend (Vue 3)                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │
│  │ ChatRoomList    │  │ ChatRoomDetail  │  │ MultiAgentMessageView   │ │
│  │   - 创建房间     │  │   - 多Agent会话  │  │   - @提及高亮           │ │
│  │   - 列表展示     │  │   - 成员管理     │  │   - Agent身份标识       │ │
│  │   - 快速进入     │  │   - 消息发送     │  │   - 消息历史            │ │
│  └────────┬────────┘  └────────┬────────┘  └─────────────────────────┘ │
│           │                    │                                        │
│           └────────────────────┼────────────────────────────────────────┘
│                                │ HTTP / WebSocket
└────────────────────────────────┼─────────────────────────────────────────┘
                                 │
┌────────────────────────────────┼─────────────────────────────────────────┐
│                         Backend (Spring Boot)                          │
│                                │                                        │
│  ┌─────────────────────────────┼─────────────────────────────────────┐  │
│  │                      ChatRoomController                           │  │
│  │  POST /api/v1/chat-rooms      - 创建聊天室                         │  │
│  │  GET  /api/v1/chat-rooms      - 列表查询                           │  │
│  │  GET  /api/v1/chat-rooms/{id} - 获取详情                           │  │
│  │  POST /api/v1/chat-rooms/{id}/messages - 发送消息                  │  │
│  └─────────────────────────────┼─────────────────────────────────────┘  │
│                                │                                        │
│  ┌─────────────────────────────┼─────────────────────────────────────┐  │
│  │                      ChatRoomService                              │  │
│  │  - CRUD操作                    │                                  │  │
│  │  - 成员管理                    │                                  │  │
│  │  - 消息历史管理                │                                  │  │
│  └─────────────────────────────┼─────────────────────────────────────┘  │
│                                │                                        │
│  ┌─────────────────────────────┼─────────────────────────────────────┐  │
│  │                    MessageRouterService                           │  │
│  │  - @提及解析                   │                                  │  │
│  │  - 消息路由决策                │                                  │  │
│  │  - 广播/单播处理               │                                  │  │
│  └─────────────────────────────┼─────────────────────────────────────┘  │
│                                │                                        │
│  ┌─────────────────────────────┼─────────────────────────────────────┐  │
│  │                    ChatRoomPushService                            │  │
│  │  - WebSocket推送               │                                  │  │
│  │  - Topic管理                   │                                  │  │
│  │  - 订阅者通知                  │                                  │  │
│  └─────────────────────────────┴─────────────────────────────────────┘  │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     Data Storage (JSON Files)                   │    │
│  │  chat_rooms.json      - 聊天室数据                              │    │
│  │  cli_agents.json      - Agent配置（已有）                       │    │
│  │  cli_agent_output_logs.json - Agent输出日志（已有）             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流图

```
用户发送消息
    │
    ▼
┌─────────────┐
│ @提及解析    │──┬── 无@ ──→ 广播给所有Agent
│             │  │
│ 提取目标Agent│  └── 有@ ──→ 仅发送给被@的Agent
└─────────────┘
    │
    ▼
┌─────────────┐
│ 消息存储     │──→ 保存到聊天室消息历史
└─────────────┘
    │
    ▼
┌─────────────┐
│ WebSocket   │──→ 推送至所有订阅者（前端用户）
│ 广播        │
└─────────────┘
    │
    ▼
┌─────────────┐
│ Agent处理   │──→ 各Agent独立处理，生成响应
└─────────────┘
    │
    ▼
┌─────────────┐
│ 响应聚合    │──→ 收集所有Agent响应
└─────────────┘
    │
    ▼
┌─────────────┐
│ 消息展示    │──→ 前端展示带Agent身份的回复
└─────────────┘
```

---

## 3. 数据模型

### 3.1 聊天室实体 (StoredChatRoom)

```java
{
    "id": "room-uuid-001",
    "name": "代码评审讨论组",
    "description": "用于多Agent代码评审和讨论",
    "createdBy": "user-001",
    "createdAt": "2026-04-07T10:00:00",
    "updatedAt": "2026-04-07T15:30:00",
    "agentIds": ["agent-001", "agent-002", "agent-003"],
    "messages": [...],  // 最近100条消息
    "active": true,
    "maxAgents": 10     // 最大Agent数量限制
}
```

### 3.2 消息实体 (ChatMessage)

```java
{
    "id": "msg-uuid-001",
    "type": "user",           // user / agent / system
    "senderId": "user-001",   // 用户ID或Agent ID
    "senderName": "张三",      // 显示名称
    "senderAvatar": "👤",      // 头像/emoji
    "content": "@CodeReviewer 请分析这段代码的质量",
    "targetAgentIds": ["agent-001"],  // @提及的目标
    "timestamp": "2026-04-07T10:05:00",
    "metadata": {
        "inputTokens": 150,
        "outputTokens": 0,
        "processingTime": 0
    }
}
```

### 3.3 Agent消息示例

```java
{
    "id": "msg-uuid-002",
    "type": "agent",
    "senderId": "agent-001",
    "senderName": "CodeReviewer",
    "senderAvatar": "🤖",
    "content": "这段代码整体结构良好，但建议优化第15行的异常处理...",
    "replyTo": "msg-uuid-001",  // 回复哪条消息
    "timestamp": "2026-04-07T10:05:30",
    "metadata": {
        "inputTokens": 150,
        "outputTokens": 320,
        "model": "claude-sonnet-4.6"
    }
}
```

---

## 4. API 设计

### 4.1 REST API

#### 聊天室管理

```yaml
# 创建聊天室
POST /api/v1/chat-rooms
Request:
  {
    "name": "讨论组名称",
    "description": "描述",
    "agentIds": ["agent-001", "agent-002"]
  }
Response: 201 Created
  { "id": "room-uuid", "name": "...", ... }

# 获取聊天室列表
GET /api/v1/chat-rooms?page=1&size=20
Response: 200 OK
  {
    "items": [...],
    "total": 50,
    "page": 1,
    "pageSize": 20
  }

# 获取聊天室详情
GET /api/v1/chat-rooms/{roomId}
Response: 200 OK
  { "id": "...", "messages": [...], ... }

# 更新聊天室
PUT /api/v1/chat-rooms/{roomId}
Request:
  {
    "name": "新名称",
    "description": "新描述"
  }

# 删除聊天室
DELETE /api/v1/chat-rooms/{roomId}
Response: 204 No Content

# 添加Agent到聊天室
POST /api/v1/chat-rooms/{roomId}/agents
Request: { "agentId": "agent-004" }

# 从聊天室移除Agent
DELETE /api/v1/chat-rooms/{roomId}/agents/{agentId}

# 获取消息历史
GET /api/v1/chat-rooms/{roomId}/messages?beforeId=xxx&limit=50
Response: 200 OK
  {
    "messages": [...],
    "hasMore": true
  }

# 发送消息
POST /api/v1/chat-rooms/{roomId}/messages
Request:
  {
    "content": "@AgentA 请帮忙",
    "type": "user"
  }
Response: 201 Created
  { "messageId": "msg-uuid", ... }
```

### 4.2 WebSocket Topic

```yaml
# 订阅聊天室消息流
/topic/chat-room/{roomId}/messages
→ 推送类型: ChatRoomMessage

# 订阅Agent状态变化
/topic/chat-room/{roomId}/agents
→ 推送类型: AgentStatusEvent

# 订阅输入状态
/topic/chat-room/{roomId}/typing
→ 推送类型: TypingEvent

# 订阅系统通知
/topic/chat-room/{roomId}/system
→ 推送类型: SystemNotification
```

### 4.3 WebSocket 消息格式

#### 聊天室消息 (ChatRoomMessage)
```json
{
    "roomId": "room-001",
    "messageId": "msg-001",
    "type": "agent",
    "senderId": "agent-001",
    "senderName": "CodeReviewer",
    "senderAvatar": "🤖",
    "content": "分析完成...",
    "targetAgentIds": [],
    "timestamp": "2026-04-07T10:05:00Z",
    "metadata": {
        "inputTokens": 150,
        "outputTokens": 200
    }
}
```

#### Agent状态事件 (AgentStatusEvent)
```json
{
    "roomId": "room-001",
    "agentId": "agent-001",
    "agentName": "CodeReviewer",
    "status": "TYPING",  // ONLINE / OFFLINE / TYPING / ERROR
    "timestamp": "2026-04-07T10:05:00Z"
}
```

---

## 5. 核心功能实现

### 5.1 @提及解析

```java
@Service
public class MentionParserService {
    
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\-\\u4e00-\\u9fa5]+)");
    
    /**
     * 解析消息中的@提及
     * @param content 消息内容
     * @param availableAgents 可用的Agent列表
     * @return 被提及的Agent ID列表
     */
    public List<String> parseMentions(String content, List<CliAgent> availableAgents) {
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String mention = matcher.group(1);
            
            // 尝试匹配Agent名称或ID
            for (CliAgent agent : availableAgents) {
                if (agent.getName().equals(mention) || 
                    agent.getId().equals(mention) ||
                    agent.getName().startsWith(mention)) {
                    mentions.add(agent.getId());
                    break;
                }
            }
        }
        
        return mentions.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 将消息内容中的@提及转换为高亮格式
     */
    public String highlightMentions(String content) {
        return MENTION_PATTERN.matcher(content)
            .replaceAll("<span class=\"mention\">@$1</span>");
    }
}
```

### 5.2 消息路由

```java
@Service
@RequiredArgsConstructor
public class MessageRouterService {
    
    private final MentionParserService mentionParser;
    private final ChatRoomPushService pushService;
    private final CliAgentService agentService;
    private final CliSessionService sessionService;
    
    /**
     * 处理用户发送的消息，决定路由方式
     */
    public void routeUserMessage(String roomId, ChatMessage message, 
                                  List<String> roomAgentIds) {
        // 1. 解析@提及
        List<CliAgent> availableAgents = agentService.getAgentsByIds(roomAgentIds);
        List<String> mentionedAgentIds = mentionParser.parseMentions(
            message.getContent(), availableAgents);
        
        message.setTargetAgentIds(mentionedAgentIds);
        
        // 2. 广播消息给所有前端订阅者
        pushService.broadcastMessage(roomId, message);
        
        // 3. 决定向哪些Agent发送
        List<String> targetAgentIds = mentionedAgentIds.isEmpty() 
            ? roomAgentIds  // 无@则广播给所有
            : mentionedAgentIds;  // 有@则只发给被@的
        
        // 4. 向目标Agent发送
        for (String agentId : targetAgentIds) {
            sendToAgent(roomId, agentId, message);
        }
    }
    
    /**
     * 向特定Agent发送消息
     */
    private void sendToAgent(String roomId, String agentId, ChatMessage message) {
        // 构建给Agent的提示词
        String agentPrompt = buildAgentPrompt(roomId, message);
        
        // 调用Agent会话服务
        sessionService.sendInput(agentId, agentPrompt);
    }
    
    /**
     * 构建Agent提示词，包含聊天室上下文
     */
    private String buildAgentPrompt(String roomId, ChatMessage message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【聊天室消息】\n");
        prompt.append("发送者: ").append(message.getSenderName()).append("\n");
        prompt.append("消息: ").append(message.getContent()).append("\n\n");
        
        // 添加聊天室上下文（最近几条消息）
        List<ChatMessage> context = getRecentContext(roomId, 5);
        if (!context.isEmpty()) {
            prompt.append("【上下文】\n");
            for (ChatMessage ctx : context) {
                prompt.append(ctx.getSenderName()).append(": ")
                      .append(ctx.getContent()).append("\n");
            }
        }
        
        return prompt.toString();
    }
}
```

### 5.3 Agent响应处理

```java
@Service
@RequiredArgsConstructor
public class AgentResponseHandler {
    
    private final ChatRoomPushService pushService;
    private final ChatRoomService chatRoomService;
    
    /**
     * 处理Agent输出，转换为聊天室消息
     */
    public void handleAgentOutput(String agentId, String output, String roomId) {
        // 查找Agent信息
        CliAgent agent = agentService.getAgent(agentId);
        
        // 构建Agent消息
        ChatMessage agentMessage = ChatMessage.builder()
            .id(UUID.randomUUID().toString())
            .type("agent")
            .senderId(agentId)
            .senderName(agent.getName())
            .senderAvatar("🤖")
            .content(output)
            .timestamp(LocalDateTime.now())
            .build();
        
        // 保存到聊天室历史
        chatRoomService.addMessage(roomId, agentMessage);
        
        // 广播给所有订阅者
        pushService.broadcastMessage(roomId, agentMessage);
    }
    
    /**
     * 处理Agent状态变化
     */
    public void handleAgentStatusChange(String agentId, String roomId, String status) {
        CliAgent agent = agentService.getAgent(agentId);
        
        AgentStatusEvent event = AgentStatusEvent.builder()
            .roomId(roomId)
            .agentId(agentId)
            .agentName(agent.getName())
            .status(status)
            .timestamp(LocalDateTime.now())
            .build();
        
        pushService.broadcastAgentStatus(roomId, event);
    }
}
```

---

## 6. 前端设计

### 6.1 页面结构

```
ChatRoomDetailView.vue
├── Header（顶部栏）
│   ├── 聊天室名称/描述
│   ├── Agent状态摘要（在线数量）
│   └── 设置按钮
├── Sidebar（左侧边栏）
│   ├── Agent列表
│   │   ├── Agent头像/名称
│   │   ├── 在线状态指示
│   │   └── 能力标签
│   └── 成员管理按钮
├── MessageArea（消息区）
│   ├── MessageBubble
│   │   ├── 用户消息（右侧，橙色）
│   │   └── Agent消息（左侧，白色 + Agent标识）
│   ├── @提及高亮
│   └── 消息时间戳
└── InputArea（输入区）
    ├── @提及自动完成
    ├── 消息输入框
    └── 发送按钮
```

### 6.2 组件设计

#### AgentMentionList（@提及下拉）

```vue
<template>
  <div v-if="showMentionList" class="mention-dropdown">
    <div
      v-for="agent in filteredAgents"
      :key="agent.id"
      class="mention-item"
      @click="selectMention(agent)"
    >
      <span class="mention-avatar">{{ agent.avatar }}</span>
      <span class="mention-name">{{ agent.name }}</span>
      <span class="mention-capability">{{ agent.capability }}</span>
    </div>
  </div>
</template>

<script setup>
// 当输入"@"时触发，过滤可用Agent列表
// 支持方向键选择和回车确认
</script>
```

#### MultiAgentMessage（多Agent消息）

```vue
<template>
  <div :class="['message', message.type]">
    <div class="message-avatar">{{ message.senderAvatar }}</div>
    <div class="message-content">
      <div class="message-header">
        <span class="sender-name">{{ message.senderName }}</span>
        <span v-if="message.type === 'agent'" class="agent-badge">Agent</span>
        <span class="message-time">{{ formatTime(message.timestamp) }}</span>
      </div>
      <div class="message-body" v-html="highlightedContent"></div>
      <div v-if="message.metadata" class="message-meta">
        <span v-if="message.metadata.outputTokens">
          {{ message.metadata.outputTokens }} tokens
        </span>
      </div>
    </div>
  </div>
</template>
```

### 6.3 状态管理 (Pinia Store)

```typescript
// stores/chatRoom.ts
export const useChatRoomStore = defineStore('chatRoom', {
  state: () => ({
    rooms: [] as ChatRoom[],
    currentRoom: null as ChatRoom | null,
    messages: [] as ChatMessage[],
    agentTyping: new Map<string, boolean>(),
    connectedAgents: [] as string[],
    hasMoreMessages: true
  }),

  actions: {
    // 加载聊天室列表
    async loadRooms() {
      this.rooms = await chatRoomApi.getRooms();
    },

    // 进入聊天室
    async enterRoom(roomId: string) {
      this.currentRoom = await chatRoomApi.getRoom(roomId);
      this.messages = await chatRoomApi.getMessages(roomId);
      this.subscribeToRoom(roomId);
    },

    // 订阅WebSocket
    subscribeToRoom(roomId: string) {
      // 订阅消息流
      ws.subscribe(`/topic/chat-room/${roomId}/messages`, (msg) => {
        this.messages.push(msg);
      });

      // 订阅Agent状态
      ws.subscribe(`/topic/chat-room/${roomId}/agents`, (event) => {
        this.updateAgentStatus(event);
      });
    },

    // 发送消息
    async sendMessage(content: string) {
      const message = await chatRoomApi.sendMessage(
        this.currentRoom!.id,
        content
      );
      this.messages.push(message);
    }
  }
});
```

---

## 7. 性能优化

### 7.1 消息分页加载

```typescript
// 前端分页加载实现
async function loadMoreMessages() {
  if (!hasMoreMessages.value || loading.value) return;

  loading.value = true;
  const beforeId = messages.value[0]?.id;
  const result = await chatRoomApi.getMessages(roomId, {
    beforeId,
    limit: 50
  });

  messages.value.unshift(...result.messages);
  hasMoreMessages.value = result.hasMore;
  loading.value = false;
}
```

### 7.2 虚拟滚动

对于消息数量较多的聊天室，使用虚拟滚动优化渲染性能：

```vue
<template>
  <VirtualList
    :items="messages"
    :item-height="80"
    v-slot="{ item }"
  >
    <MultiAgentMessage :message="item" />
  </VirtualList>
</template>
```

### 7.3 消息防抖

Agent输出频繁更新时，使用防抖减少渲染次数：

```typescript
const debouncedUpdate = debounce((content: string) => {
  updateMessageContent(content);
}, 100);
```

---

## 8. 安全考虑

### 8.1 输入过滤

```java
@Service
public class ContentFilterService {
    
    public String sanitize(String content) {
        // 1. 转义HTML特殊字符
        String safe = HtmlUtils.htmlEscape(content);
        
        // 2. 过滤危险协议
        safe = safe.replaceAll("(?i)javascript:", "");
        safe = safe.replaceAll("(?i)data:text/html", "");
        
        // 3. 限制消息长度
        if (safe.length() > 10000) {
            safe = safe.substring(0, 10000) + "...";
        }
        
        return safe;
    }
}
```

### 8.2 权限控制

```java
@RestController
@PreAuthorize("hasRole('USER')")
public class ChatRoomController {
    
    @PostMapping("/chat-rooms/{id}/messages")
    @PreAuthorize("@chatRoomPermissionService.canSend(#id, authentication)")
    public ResponseEntity<?> sendMessage(@PathVariable String id, ...) {
        // ...
    }
}
```

---

## 9. 测试策略

### 9.1 单元测试

```java
@ExtendWith(MockitoExtension.class)
class MentionParserServiceTest {
    
    @Test
    void shouldParseMentions() {
        String content = "@Agent1 @Agent2 请帮忙";
        List<CliAgent> agents = List.of(
            CliAgent.builder().id("1").name("Agent1").build(),
            CliAgent.builder().id("2").name("Agent2").build()
        );
        
        List<String> mentions = parser.parseMentions(content, agents);
        
        assertThat(mentions).containsExactly("1", "2");
    }
}
```

### 9.2 集成测试

```java
@SpringBootTest
class ChatRoomIntegrationTest {
    
    @Test
    void shouldBroadcastMessageToAllAgents() {
        // 创建聊天室
        ChatRoom room = chatRoomService.createRoom(...);
        
        // 添加多个Agent
        chatRoomService.addAgent(room.getId(), "agent-1");
        chatRoomService.addAgent(room.getId(), "agent-2");
        
        // 发送广播消息
        chatRoomService.sendMessage(room.getId(), "大家好");
        
        // 验证所有Agent都收到消息
        verify(sessionService).sendInput(eq("agent-1"), any());
        verify(sessionService).sendInput(eq("agent-2"), any());
    }
}
```

---

## 10. 部署与运维

### 10.1 数据库迁移

新增JSON存储文件无需迁移，系统自动创建。

### 10.2 监控指标

```yaml
metrics:
  - chat_room.active_count      # 活跃聊天室数量
  - chat_room.message_rate      # 消息发送速率
  - chat_room.agent_response_time  # Agent响应时间
  - websocket.connection_count  # WebSocket连接数
```

### 10.3 故障恢复

- **消息丢失**: 本地存储 + 重试机制
- **WebSocket断开**: 自动重连 + 消息补发
- **Agent崩溃**: 状态监控 + 自动重启

---

## 附录

### A. 术语表

| 术语 | 说明 |
|------|------|
| ChatRoom | 多Agent聊天室，容纳多个Agent和用户会话的容器 |
| @提及 | 通过@符号指定特定Agent参与对话 |
| 广播 | 消息发送给聊天室内所有Agent |
| 消息路由 | 决定消息应该发送给哪些Agent的逻辑 |

### B. 参考文档

- [CLAUDE.md](../../CLAUDE.md) - 项目架构文档
- [feature-list.json](../../feature-list.json) - 功能清单
- [Spring WebSocket Docs](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket) - WebSocket参考

---

**文档结束**
