# Platform Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 系统性重构项目：删除6个未使用模块和登录系统，重组前后端包结构，拆分大组件，补全测试。

**Architecture:** 聚焦3个核心子系统（CLI Agent、群聊、Dashboard）。后端消除过度抽象（接口/实现合并），前端拆大组件为小组件，API 模块 TypeScript 化。

**Tech Stack:** Spring Boot 3.2 / Java 17 / Vue 3.4 / TypeScript 5 / Element Plus 2.5 / Vite 5

---

### Task 1: 删除后端已废弃模块

**Files:**
- Delete: `cat-standalone/src/main/java/com/cat/standalone/controller/TaskController.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/controller/AgentController.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/controller/AgentMessageController.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/controller/SimpleAuthController.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/service/LocalTaskService.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/service/LocalAgentService.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/service/LocalAgentMessageService.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/config/SimpleAuthInterceptor.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/dto/CreateTaskRequest.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/dto/UpdateTaskRequest.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/dto/TaskQuery.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/dto/TaskResponse.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/dto/TaskLogResponse.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/entity/TaskAssignment.java`
- Delete: `cat-standalone/src/main/java/com/cat/task/service/TaskService.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/dto/CreateAgentRequest.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/dto/UpdateAgentRequest.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/dto/AgentQuery.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/dto/AgentResponse.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/entity/AgentCapability.java`
- Delete: `cat-standalone/src/main/java/com/cat/agent/service/AgentService.java`
- Delete: `cat-standalone/src/main/java/com/cat/cliagent/service/AgentMessageService.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredAgent.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTask.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredAgentCapability.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTaskAssignment.java`
- Delete: `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTaskLog.java`
- Modify: `cat-standalone/src/main/java/com/cat/standalone/store/StoreConfig.java`

- [ ] **Step 1: 删除所有已废弃的 Java 文件**

```bash
# 删除 Controller 层
rm cat-standalone/src/main/java/com/cat/standalone/controller/TaskController.java
rm cat-standalone/src/main/java/com/cat/standalone/controller/AgentController.java
rm cat-standalone/src/main/java/com/cat/standalone/controller/AgentMessageController.java
rm cat-standalone/src/main/java/com/cat/standalone/controller/SimpleAuthController.java

# 删除 Service 层（本地实现）
rm cat-standalone/src/main/java/com/cat/standalone/service/LocalTaskService.java
rm cat-standalone/src/main/java/com/cat/standalone/service/LocalAgentService.java
rm cat-standalone/src/main/java/com/cat/standalone/service/LocalAgentMessageService.java

# 删除 Auth 配置
rm cat-standalone/src/main/java/com/cat/standalone/config/SimpleAuthInterceptor.java

# 删除 task package 全部文件
rm -r cat-standalone/src/main/java/com/cat/task/

# 删除 agent package 全部文件  
rm -r cat-standalone/src/main/java/com/cat/agent/

# 删除 AgentMessage 接口
rm cat-standalone/src/main/java/com/cat/cliagent/service/AgentMessageService.java

# 删除已废弃的 Entity
rm cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredAgent.java
rm cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTask.java
rm cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredAgentCapability.java
rm cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTaskAssignment.java
rm cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTaskLog.java
```

- [ ] **Step 2: 更新 StoreConfig.java 移除废弃 Bean**

删除以下 Bean 定义:
- `agentStore()`
- `taskStore()`
- `capabilityStore()`
- `assignmentStore()`
- `taskLogStore()`

同时删除对应 import:
- `com.cat.standalone.store.entity.StoredAgent`
- `com.cat.standalone.store.entity.StoredTask`
- `com.cat.standalone.store.entity.StoredAgentCapability`
- `com.cat.standalone.store.entity.StoredTaskAssignment`
- `com.cat.standalone.store.entity.StoredTaskLog`

- [ ] **Step 3: 验证后端编译通过**

```bash
cd cat-standalone
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: remove deprecated modules (Task, Agent, AgentMessage, Auth)

删除未使用的 Task/Agent/AgentMessage/Auth 模块，清理对应 Entity 和 Store Bean。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 删除前端已废弃模块

**Files:**
- Delete: `cat-web/src/views/user/UserManagementView.vue`
- Delete: `cat-web/src/views/user/RoleManagementView.vue`
- Delete: `cat-web/src/views/agent/AgentDetailView.vue`
- Delete: `cat-web/src/views/agent/AgentListView.vue`
- Delete: `cat-web/src/views/login/LoginView.vue`
- Delete: `cat-web/src/api/user.ts`
- Delete: `cat-web/src/api/agent.ts`
- Delete: `cat-web/src/api/auth.ts`
- Delete: `cat-web/src/stores/auth.ts`
- Delete: `cat-web/src/types/auth.ts`
- Modify: `cat-web/src/router/index.ts`

- [ ] **Step 1: 删除废弃的前端文件**

```bash
# 删除废弃视图
rm cat-web/src/views/user/UserManagementView.vue
rm cat-web/src/views/user/RoleManagementView.vue
rm cat-web/src/views/agent/AgentDetailView.vue
rm cat-web/src/views/agent/AgentListView.vue
rm cat-web/src/views/login/LoginView.vue

# 删除废弃 API 模块
rm cat-web/src/api/user.ts
rm cat-web/src/api/agent.ts
rm cat-web/src/api/auth.ts

# 删除废弃 store/types
rm cat-web/src/stores/auth.ts
rm cat-web/src/types/auth.ts

# 删除空目录
rmdir cat-web/src/views/user 2>/dev/null; true
rmdir cat-web/src/views/agent 2>/dev/null; true
rmdir cat-web/src/views/login 2>/dev/null; true
```

- [ ] **Step 2: 精简路由配置**

将 `cat-web/src/router/index.ts` 替换为：

```typescript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
      },
      {
        path: 'cli-agents',
        name: 'CliAgentList',
        component: () => import('@/views/cliAgent/CliAgentListView.vue')
      },
      {
        path: 'cli-agents/:id',
        name: 'CliAgentDetail',
        component: () => import('@/views/cliAgent/CliAgentDetailView.vue')
      },
      {
        path: 'group-chat',
        name: 'GroupChat',
        component: () => import('@/views/groupChat/GroupChatView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

- [ ] **Step 3: 更新 AppLayout.vue 移除登录依赖**

删除 `AppLayout.vue` 中的：
- import `useAuthStore` from `@/stores/auth`
- `authStore`、`displayName`、`handleCommand` 相关代码
- `el-dropdown` 用户下拉菜单（替换为静态头像展示）
- 登出相关逻辑

将 header-right 区域改为：
```vue
<div class="header-right">
  <span class="avatar-ring">
    <el-avatar :size="30" class="user-avatar">U</el-avatar>
  </span>
</div>
```

- [ ] **Step 4: 更新 main.ts 移除 auth store 初始化**

删除 `main.ts` 中 auth store 相关 import 和初始化代码（如果有）。

- [ ] **Step 5: 验证前端编译通过**

```bash
cd cat-web
npm run build
```

Expected: 无错误输出

- [ ] **Step 6: 提交**

```bash
git add -A
git commit -m "refactor: remove deprecated frontend modules (User, Role, Agent, Login, Auth)

删除未使用的用户管理/角色管理/Agent管理/登录页面，
精简路由为4条，移除 auth store 依赖。

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 后端包结构重组 — 合并 standalone 到顶层

**Files:**
- Move: `com.cat.standalone.controller.*` → `com.cat.controller.*`
- Move: `com.cat.standalone.service.*` → `com.cat.cliagent.*` or `com.cat.chatgroup.*`
- Move: `com.cat.standalone.store.*` → `com.cat.store.*`
- Move: `com.cat.standalone.config.*` → `com.cat.config.*`
- Modify: `CatStandaloneApplication.java` → `CatApplication.java`

- [ ] **Step 1: 创建新包目录**

```bash
mkdir -p cat-standalone/src/main/java/com/cat/controller
mkdir -p cat-standalone/src/main/java/com/cat/config
mkdir -p cat-standalone/src/main/java/com/cat/store
mkdir -p cat-standalone/src/main/java/com/cat/store/entity
mkdir -p cat-standalone/src/main/java/com/cat/cliagent
mkdir -p cat-standalone/src/main/java/com/cat/cliagent/dto
mkdir -p cat-standalone/src/main/java/com/cat/cliagent/entity
mkdir -p cat-standalone/src/main/java/com/cat/chatgroup
mkdir -p cat-standalone/src/main/java/com/cat/chatgroup/entity
mkdir -p cat-standalone/src/main/java/com/cat/dashboard
```

- [ ] **Step 2: 移动 Controller 文件并修改包名**

将以下文件移动到 `com.cat.controller` 包，修改 `package` 声明和 import：

| 原路径 | 新路径 |
|--------|--------|
| `com.cat.standalone.controller.CliAgentController` | `com.cat.controller.CliAgentController` |
| `com.cat.standalone.controller.ChatGroupController` | `com.cat.controller.ChatGroupController` |
| `com.cat.standalone.controller.DashboardController` | `com.cat.controller.DashboardController` |
| `com.cat.standalone.controller.CliAgentTemplateController` | `com.cat.controller.CliAgentTemplateController` |
| `com.cat.standalone.controller.CliAgentCapabilityController` | `com.cat.controller.CliAgentCapabilityController` |
| `com.cat.standalone.controller.CliAgentMonitorController` | `com.cat.controller.CliAgentMonitorController` |

每个文件需要：
1. 修改 `package` 声明为 `com.cat.controller`
2. 更新 import（替换 `com.cat.standalone.service.X` 为 `com.cat.cliagent.X` 或 `com.cat.chatgroup.X`）

以 CliAgentController 为例，修改 package 声明：

```java
package com.cat.controller;

import com.cat.cliagent.dto.*;
import com.cat.cliagent.CliAgentService;
import com.cat.cliagent.CliProcessService;
import com.cat.cliagent.CliSessionService;
import com.cat.cliagent.CliTaskExecutionService;
import com.cat.cliagent.TokenUsageService;
// ... 其他 import
```

- [ ] **Step 3: 移动并合并 Service 文件**

**原则**：消除接口/实现分离。以 CliAgentController 依赖的服务为例：

**CliAgentService** — 将 `LocalCliAgentService` 移到 `com.cat.cliagent`，改名为 `CliAgentService`（删除原 interface `com.cat.cliagent.service.CliAgentService`）

每个 Service 用具体代码替代。移动文件并修改包名：

```java
// com/cat/cliagent/CliAgentService.java (原 LocalCliAgentService)
package com.cat.cliagent;

import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredCliAgent;
// ...
@Service
public class CliAgentService {
    // 原 LocalCliAgentService 的全部逻辑
}
```

需要处理的 Service 映射：

| 原实现类 | 新类名 | 新包 | 同时删除的接口 |
|----------|--------|------|---------------|
| `LocalCliAgentService` | `CliAgentService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliAgentService` |
| `LocalCliProcessService` | `CliProcessService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliProcessService` |
| `LocalCliSessionService` | `CliSessionService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliSessionService` |
| `LocalCliTaskExecutionService` | `CliTaskExecutionService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliTaskExecutionService` |
| `LocalTokenUsageService` | `TokenUsageService` | `com.cat.cliagent` | `com.cat.cliagent.service.TokenUsageService` |
| `LocalCliOutputPushService` | `CliOutputPushService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliOutputPushService` |
| `LocalCliAgentMonitorService` | `CliAgentMonitorService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliAgentMonitorService` |
| `LocalCliAgentTemplateService` | `CliAgentTemplateService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliAgentTemplateService` |
| `LocalCliAgentCapabilityService` | `CliAgentCapabilityService` | `com.cat.cliagent` | `com.cat.cliagent.service.CliAgentCapabilityService` |
| `LocalChatGroupService` | `ChatGroupService` | `com.cat.chatgroup` | `com.cat.cliagent.service.ChatGroupService` |

- [ ] **Step 4: 移动 Config 文件**

移动并修改包名：
- `com.cat.standalone.config.WebConfig` → `com.cat.config.WebConfig`
- `com.cat.standalone.config.WebSocketConfig` → `com.cat.config.WebSocketConfig`
- `com.cat.standalone.config.GlobalExceptionHandler` → `com.cat.config.GlobalExceptionHandler`
- `com.cat.standalone.store.StoreConfig` → `com.cat.config.StoreConfig`

- [ ] **Step 5: 移动 Store 文件**

| 原路径 | 新路径 |
|--------|--------|
| `com.cat.standalone.store.JsonFileStore` | `com.cat.store.JsonFileStore` |
| `com.cat.standalone.store.entity.StoredCliAgent` | `com.cat.store.entity.StoredCliAgent` |
| `com.cat.standalone.store.entity.StoredCliAgentTemplate` | `com.cat.store.entity.StoredCliAgentTemplate` |
| `com.cat.standalone.store.entity.StoredCliAgentCapability` | `com.cat.store.entity.StoredCliAgentCapability` |
| `com.cat.standalone.store.entity.StoredCliAgentOutputLog` | `com.cat.store.entity.StoredCliAgentOutputLog` |
| `com.cat.standalone.store.entity.StoredTokenUsageLog` | `com.cat.store.entity.StoredTokenUsageLog` |
| `com.cat.standalone.store.entity.StoredChatGroup` | `com.cat.chatgroup.entity.StoredChatGroup` |
| `com.cat.standalone.store.entity.StoredChatGroupMessage` | `com.cat.chatgroup.entity.StoredChatGroupMessage` |

- [ ] **Step 6: 移动 DTO 文件到对应包**

Service DTO（作为 Service 的内部 record/class）直接定义在 Service 类中，放在对应包的 `dto/` 子目录：

| 原路径 | 新路径 |
|--------|--------|
| `com.cat.cliagent.dto.*` | `com.cat.cliagent.dto.*` (保持，但只保留实际使用的) |

不用的 DTO 删除：
- `com.cat.cliagent.dto.CliAgentMonitorStatus` — 仅 monitor service 使用，保留
- 检查每个 DTO 是否被引用，删除未被引用的

- [ ] **Step 7: 更新 CatApplication.java**

重命名 `CatStandaloneApplication.java` → `CatApplication.java`，更新 package 和 import：

```java
package com.cat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cat")
public class CatApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatApplication.class, args);
    }
}
```

- [ ] **Step 8: 更新 WebConfig.java 移除 Auth 拦截器引用**

删除 WebConfig 中关于 SimpleAuthInterceptor 的拦截器注册代码（如果有）。

- [ ] **Step 9: 清理空目录**

```bash
# 删除原 standalone 包下已迁移完的空目录
rmdir cat-standalone/src/main/java/com/cat/standalone/controller 2>/dev/null; true
rmdir cat-standalone/src/main/java/com/cat/standalone/service 2>/dev/null; true
rmdir cat-standalone/src/main/java/com/cat/standalone 2>/dev/null; true
```

- [ ] **Step 10: 验证后端编译通过**

```bash
cd cat-standalone
mvn clean compile
```

Expected: BUILD SUCCESS

- [ ] **Step 11: 提交**

```bash
git add -A
git commit -m "refactor: restructure backend packages, eliminate interface/impl separation

- 合并 standalone 包到顶层 (controller/config/store/cliagent/chatgroup/dashboard)
- 消除接口/实现分离：LocalXxxService 直接重命名为 XxxService
- 删除未使用的 DTO 和废弃接口
- CatStandaloneApplication 重命名为 CatApplication

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 前端组件拆分 — 群聊模块

**Files:**
- Create: `cat-web/src/views/groupChat/components/ChatSidebar.vue`
- Create: `cat-web/src/views/groupChat/components/ChatMessageList.vue`
- Create: `cat-web/src/views/groupChat/components/ChatInput.vue`
- Create: `cat-web/src/views/groupChat/components/MentionPopup.vue`
- Create: `cat-web/src/views/groupChat/components/GroupCreateDialog.vue`
- Create: `cat-web/src/views/groupChat/composables/useChatWebSocket.ts`
- Create: `cat-web/src/types/models.ts`
- Modify: `cat-web/src/views/groupChat/GroupChatView.vue`

- [ ] **Step 1: 创建共享类型文件**

创建 `cat-web/src/types/models.ts`：

```typescript
// 共享类型定义

export interface AgentBrief {
  id: string
  name: string
  status: string
}

export interface ChatGroup {
  id: string
  name: string
  description: string
  agentIds: string[]
  agents: AgentBrief[]
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  groupId: string
  senderType: 'user' | 'agent' | 'system'
  senderAgentId?: string
  senderName: string
  content: string
  mentionedAgentIds: string[]
  broadcast: boolean
  createdAt: string
}

export interface CliAgent {
  id: string
  name: string
  description: string
  templateId: string
  templateName: string
  cliType: string
  status: string
  executablePath: string
  configPath: string
  args: string[]
  envVars: Record<string, string>
  workingDir: string
  processId: string
  capabilities: any[]
  lastStartedAt: string
}

export interface GroupForm {
  name: string
  description: string
  agentIds: string[]
}

export interface GroupMessagePayload {
  content: string
  mentionedAgentIds?: string[] | null
}
```

- [ ] **Step 2: 创建 ChatSidebar.vue**

创建 `cat-web/src/views/groupChat/components/ChatSidebar.vue`：

```vue
<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <h3>群聊</h3>
      <el-button size="small" type="primary" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>

    <div class="group-list">
      <div
        v-for="group in groups"
        :key="group.id"
        :class="['group-item', { active: selectedId === group.id }]"
        @click="$emit('select', group)"
      >
        <div class="group-avatar">👥</div>
        <div class="group-info">
          <div class="group-name">{{ group.name }}</div>
          <div class="group-meta">{{ group.agents?.length || 0 }} 个Agent</div>
        </div>
        <el-dropdown @command="(cmd: string) => $emit('action', cmd, group)" trigger="click" @click.stop>
          <el-button text size="small">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-empty v-if="groups.length === 0" description="暂无群组，请创建" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, MoreFilled } from '@element-plus/icons-vue'
import type { ChatGroup } from '@/types/models'

defineProps<{
  groups: ChatGroup[]
  selectedId: string | undefined
}>()

defineEmits<{
  select: [group: ChatGroup]
  create: []
  action: [command: string, group: ChatGroup]
}>()
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-sidebar {
  width: 280px;
  background: $bg-deep;
  border-right: 1px solid $border-subtle;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 { margin: 0; font-size: 16px; color: $text-primary; }
}

.group-list { flex: 1; overflow-y: auto; }

.group-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(124, 58, 237, 0.05);
  transition: background 0.2s;
  position: relative;

  &:hover { background: $bg-hover; }

  &.active {
    background: $bg-surface;
    &::before {
      content: '';
      position: absolute;
      left: 0; top: 8px; bottom: 8px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: linear-gradient(180deg, $color-violet, $color-cyan);
    }
  }
}

.group-avatar {
  width: 40px; height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}

.group-info { flex: 1; min-width: 0; }
.group-name {
  font-weight: 600; font-size: 14px; color: $text-primary;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.group-meta { font-size: 12px; color: $text-muted; }
</style>
```

- [ ] **Step 3: 创建 ChatMessageList.vue**

创建 `cat-web/src/views/groupChat/components/ChatMessageList.vue`：

```vue
<template>
  <div class="chat-messages" ref="messagesRef">
    <div v-if="messages.length === 0 && Object.keys(activeOutputs).length === 0" class="empty-chat">
      <div class="empty-icon">💬</div>
      <p>开始群聊吧！</p>
      <p class="hint">发送消息给所有Agent，或在输入框中输入 @ 指定Agent</p>
    </div>

    <div v-for="(msg, index) in messages" :key="msg.id || index" :class="['message', msg.senderType]">
      <div class="message-avatar">{{ getMessageAvatar(msg) }}</div>
      <div class="message-content">
        <div class="message-header">
          <span class="sender" :class="'sender-' + msg.senderType">{{ msg.senderName || '未知' }}</span>
          <span v-if="msg.broadcast" class="broadcast-tag">📢 广播</span>
          <span v-if="msg.mentionedAgentIds?.length" class="mention-tag">
            @{{ getMentionNames(msg.mentionedAgentIds) }}
          </span>
          <span class="time">{{ formatTime(msg.createdAt) }}</span>
        </div>
        <div class="message-text" v-html="formatMessage(msg.content)"></div>
      </div>
    </div>

    <div v-for="(output, agentId) in activeOutputs" :key="'output-' + agentId" class="message agent streaming">
      <div class="message-avatar">🤖</div>
      <div class="message-content">
        <div class="message-header">
          <span class="sender sender-agent">{{ getAgentName(agentId) }}</span>
          <span class="streaming-indicator">
            <span class="spinner-icon">{{ spinnerFrame }}</span> 输出中...
          </span>
        </div>
        <div class="message-text" v-html="formatMessage(output)"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { ChatMessage } from '@/types/models'

const props = defineProps<{
  messages: ChatMessage[]
  activeOutputs: Record<string, string>
  spinnerFrame: string
  agents: { id: string; name: string }[]
}>()

const messagesRef = ref<HTMLElement | null>(null)

watch(() => props.messages.length, () => {
  nextTick(() => scrollToBottom())
})

watch(() => Object.keys(props.activeOutputs).length, () => {
  nextTick(() => scrollToBottom())
})

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function getMessageAvatar(msg: ChatMessage): string {
  if (msg.senderType === 'user') return '👤'
  if (msg.senderType === 'system') return 'ℹ️'
  return '🤖'
}

function getAgentName(agentId: string): string {
  return props.agents.find(a => a.id === agentId)?.name || agentId
}

function getMentionNames(agentIds: string[]): string {
  return agentIds.map(id => getAgentName(id)).join(', ')
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  try { return new Date(dateStr).toLocaleTimeString() } catch { return dateStr }
}

function formatMessage(content: string): string {
  if (!content) return '<span class="empty-content">...</span>'
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  return escaped
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px; background: $bg-base;
}

.empty-chat {
  text-align: center; padding: 60px 20px; color: $text-muted;
  .empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.5; }
  p { color: $text-secondary; }
  .hint { font-size: 12px; color: $text-muted; }
}

.message { display: flex; gap: 12px; margin-bottom: 16px; }

.message-avatar {
  width: 36px; height: 36px;
  background: $bg-surface; border-radius: $radius-sm;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0; border: 1px solid $border-subtle;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  border-color: transparent;
}

.message.system .message-avatar {
  background: $bg-hover; border-color: $border-subtle;
}

.message-content { flex: 1; min-width: 0; }

.message-header {
  display: flex; align-items: center; gap: 8px; margin-bottom: 4px; flex-wrap: wrap;
}

.sender { font-weight: 600; font-size: 13px; }
.sender-user {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text;
}
.sender-agent { color: $status-running; }
.sender-system { color: $text-muted; }

.broadcast-tag { font-size: 11px; color: $warning; }
.mention-tag { font-size: 11px; color: $color-violet; }
.time { font-size: 11px; color: $text-muted; }

.message-text {
  background: $bg-surface; padding: 10px 14px; border-radius: $radius-md;
  font-size: 14px; line-height: 1.6; border: 1px solid $border-subtle;
  color: $text-primary; word-break: break-word;

  :deep(code) {
    background: $bg-hover; padding: 1px 5px; border-radius: 4px;
    font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 13px;
  }
}

.message.user .message-text {
  background: rgba(124, 58, 237, 0.08); border-color: rgba(124, 58, 237, 0.15);
}

.message.system .message-text {
  background: $bg-hover; border-color: $border-subtle; font-size: 13px; color: $text-secondary;
}

.message.agent .message-text {
  background: $bg-surface; border-color: $border-subtle; border-left: 3px solid $color-violet;
}

.message.streaming .message-text {
  border-color: rgba(124, 58, 237, 0.3);
  animation: breathe 2s infinite;
}

.streaming-indicator {
  font-size: 12px; color: $color-violet; display: flex; align-items: center; gap: 4px;
}

.spinner-icon { font-weight: bold; color: $color-violet; }
.empty-content { color: $text-muted; }

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
```

- [ ] **Step 4: 创建 ChatInput.vue**

创建 `cat-web/src/views/groupChat/components/ChatInput.vue`：

```vue
<template>
  <div class="chat-input">
    <!-- @提及弹窗 -->
    <MentionPopup
      v-if="showMentionPopup"
      :agents="mentionAtPosition >= 0 ? filteredMentionAgents : agents"
      :mentionedAgentIds="mentionedAgentIds"
      :highlightIndex="mentionHighlightIndex"
      :mode="mentionAtPosition >= 0 ? 'filter' : 'select'"
      @toggle="toggleMention"
      @selectFromFilter="selectMentionFromInput"
      @close="closeMentionPopupFromInput"
    />

    <!-- 已@的Agent标签 -->
    <div v-if="mentionedAgentIds.length > 0" class="mention-tags">
      <el-tag
        v-for="agentId in mentionedAgentIds" :key="agentId"
        closable size="small" type="primary"
        @close="$emit('removeMention', agentId)"
      >
        @{{ getAgentName(agentId) }}
      </el-tag>
      <el-button text size="small" @click="$emit('clearMentions')">清除全部</el-button>
    </div>

    <div class="input-row">
      <el-button
        :type="showMentionPopup ? 'primary' : 'default'"
        size="small" @click="toggleMentionPopupButton" class="mention-btn"
      >@</el-button>
      <el-input
        ref="inputRef"
        v-model="inputText"
        type="textarea" :rows="3"
        :placeholder="placeholder"
        @keydown="handleKeydown"
      />
    </div>
    <div class="input-actions">
      <div class="input-hints">
        <span v-if="mentionedAgentIds.length > 0">
          📌 消息将发送给 {{ mentionedAgentIds.length }} 个Agent
        </span>
        <span v-else>📢 消息将广播给所有Agent · 按 Enter 发送</span>
      </div>
      <el-button
        type="primary" @click="$emit('send')"
        :loading="isSending" :disabled="!inputText.trim()"
      >发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import MentionPopup from './MentionPopup.vue'
import type { AgentBrief } from '@/types/models'

const props = defineProps<{
  agents: AgentBrief[]
  mentionedAgentIds: string[]
  isSending: boolean
}>()

const emit = defineEmits<{
  send: []
  toggleMention: [agentId: string]
  removeMention: [agentId: string]
  clearMentions: []
  selectMentionFromInput: [agentId: string]
}>()

const inputText = ref('')
const showMentionPopup = ref(false)
const mentionFilter = ref('')
const mentionAtPosition = ref(-1)
const mentionHighlightIndex = ref(0)
const inputRef = ref<any>(null)

const placeholder = computed(() => {
  if (props.mentionedAgentIds.length > 0) return '输入消息发送给指定Agent... (Enter发送)'
  return '输入消息广播给所有Agent... (Enter发送，输入@指定Agent)'
})

const filteredMentionAgents = computed(() => {
  const filter = mentionFilter.value.toLowerCase()
  if (!filter) return props.agents
  return props.agents.filter(a => a.name.toLowerCase().includes(filter))
})

function getAgentName(agentId: string): string {
  return props.agents.find(a => a.id === agentId)?.name || agentId
}

watch(inputText, () => {
  nextTick(() => handleInputChange())
})

function handleInputChange() {
  const textareaEl = inputRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
  if (!textareaEl) return
  const value = inputText.value
  const cursorPos = textareaEl.selectionStart || value.length
  const textBefore = value.substring(0, cursorPos)
  const lastAtIndex = textBefore.lastIndexOf('@')

  if (lastAtIndex >= 0) {
    const charBefore = lastAtIndex > 0 ? textBefore[lastAtIndex - 1] : ' '
    if (charBefore === ' ' || charBefore === '\n' || lastAtIndex === 0) {
      const query = textBefore.substring(lastAtIndex + 1)
      if (!query.includes(' ') && !query.includes('\n')) {
        mentionFilter.value = query
        mentionAtPosition.value = lastAtIndex
        mentionHighlightIndex.value = 0
        showMentionPopup.value = true
        return
      }
    }
  }
  if (mentionAtPosition.value >= 0) closeMentionPopupFromInput()
}

function handleKeydown(event: KeyboardEvent) {
  if (showMentionPopup.value && mentionAtPosition.value >= 0) {
    const agents = filteredMentionAgents.value
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      mentionHighlightIndex.value = (mentionHighlightIndex.value + 1) % agents.length
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      mentionHighlightIndex.value = (mentionHighlightIndex.value - 1 + agents.length) % agents.length
    } else if (event.key === 'Enter' && agents.length > 0) {
      event.preventDefault()
      selectMentionFromInput(agents[mentionHighlightIndex.value].id)
    } else if (event.key === 'Escape') {
      event.preventDefault()
      closeMentionPopupFromInput()
    }
    return
  }
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    emit('send')
  }
}

function selectMentionFromInput(agentId: string) {
  emit('selectMentionFromInput', agentId)
  const atPos = mentionAtPosition.value
  if (atPos >= 0) {
    const before = inputText.value.substring(0, atPos)
    const afterCursor = inputText.value.substring(atPos + 1 + mentionFilter.value.length)
    inputText.value = before + afterCursor
  }
  closeMentionPopupFromInput()
}

function closeMentionPopupFromInput() {
  mentionFilter.value = ''
  mentionAtPosition.value = -1
  mentionHighlightIndex.value = 0
  showMentionPopup.value = false
}

function toggleMentionPopupButton() {
  if (mentionAtPosition.value >= 0) { closeMentionPopupFromInput(); return }
  showMentionPopup.value = !showMentionPopup.value
}

function toggleMention(agentId: string) {
  emit('toggleMention', agentId)
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-input {
  padding: 12px 20px; border-top: 1px solid $border-subtle;
  background: $bg-surface; position: relative;
}

.mention-tags {
  padding: 4px 8px; display: flex; align-items: center; gap: 4px;
  flex-wrap: wrap; border-bottom: 1px solid $border-subtle;
}

.input-row { display: flex; gap: 8px; align-items: flex-start; }

.mention-btn {
  font-weight: bold; font-size: 16px; min-width: 36px; flex-shrink: 0; margin-top: 4px;
}

.input-actions {
  display: flex; justify-content: space-between; align-items: center; margin-top: 8px;
}

.input-hints { font-size: 12px; color: $text-muted; }
</style>
```

- [ ] **Step 5: 创建 MentionPopup.vue**

创建 `cat-web/src/views/groupChat/components/MentionPopup.vue`：

```vue
<template>
  <div class="mention-popup">
    <div class="mention-header">
      {{ mode === 'filter' ? '输入名称筛选Agent：' : '选择要@的Agent：' }}
    </div>
    <div
      v-for="(agent, idx) in agents"
      :key="agent.id"
      :class="['mention-item', {
        selected: mentionedAgentIds.includes(agent.id),
        highlighted: mode === 'filter' && idx === highlightIndex
      }]"
      @click="mode === 'filter' ? $emit('selectFromFilter', agent.id) : $emit('toggle', agent.id)"
    >
      <span class="mention-avatar">🤖</span>
      <span class="mention-name">{{ agent.name }}</span>
      <el-tag size="small" :type="getAgentTagType(agent.status)">{{ getAgentStatusText(agent.status) }}</el-tag>
      <el-icon v-if="mentionedAgentIds.includes(agent.id)" class="mention-check"><Check /></el-icon>
    </div>
    <div v-if="agents.length === 0 && mode === 'filter'" class="mention-empty">无匹配的Agent</div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import type { AgentBrief } from '@/types/models'

defineProps<{
  agents: AgentBrief[]
  mentionedAgentIds: string[]
  highlightIndex: number
  mode: 'select' | 'filter'
}>()

defineEmits<{
  toggle: [agentId: string]
  selectFromFilter: [agentId: string]
  close: []
}>()

function getAgentTagType(status: string): string {
  const map: Record<string, string> = { RUNNING: 'success', EXECUTING: 'primary', STOPPED: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}

function getAgentStatusText(status: string): string {
  const map: Record<string, string> = { RUNNING: '运行中', EXECUTING: '执行中', STOPPED: '已停止', ERROR: '错误' }
  return map[status] || status
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.mention-popup {
  position: absolute; bottom: 100%; left: 0; right: 0;
  background: $bg-elevated; border: 1px solid $border-active;
  border-radius: $radius-md; box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.3);
  max-height: 200px; overflow-y: auto; z-index: 10;
}

.mention-header {
  padding: 8px 12px; font-size: 12px; color: $text-muted; border-bottom: 1px solid $border-subtle;
}

.mention-item {
  padding: 8px 12px; display: flex; align-items: center; gap: 8px;
  cursor: pointer; transition: background 0.2s; color: $text-secondary;
  &:hover { background: $bg-hover; }
  &.selected { background: $color-violet-dim; }
  &.highlighted { background: $color-violet-dim; outline: 1px solid $color-violet; outline-offset: -1px; }
}

.mention-empty { padding: 12px; text-align: center; color: $text-muted; font-size: 13px; }
.mention-avatar { font-size: 16px; }
.mention-name { flex: 1; font-size: 13px; color: $text-primary; }
.mention-check { color: $color-violet; }
</style>
```

- [ ] **Step 6: 创建 GroupCreateDialog.vue**

创建 `cat-web/src/views/groupChat/components/GroupCreateDialog.vue`：

```vue
<template>
  <el-dialog v-model="visible" :title="editing ? '编辑群组' : '创建群组'" width="500px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="群组名称" required>
        <el-input v-model="form.name" placeholder="输入群组名称" />
      </el-form-item>
      <el-form-item label="群组描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="输入群组描述" />
      </el-form-item>
      <el-form-item label="选择Agent" required>
        <div class="agent-selector">
          <el-checkbox-group v-model="form.agentIds">
            <div v-for="agent in allAgents" :key="agent.id" class="agent-checkbox-item">
              <el-checkbox :value="agent.id">
                <span class="agent-checkbox-label">
                  🤖 {{ agent.name }}
                  <el-tag size="small" :type="getAgentTagType(agent.status)">{{ getAgentStatusText(agent.status) }}</el-tag>
                </span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="allAgents.length === 0" description="暂无Agent，请先创建" :image-size="40" />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="$emit('save')" :loading="saving">
        {{ editing ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { GroupForm, AgentBrief } from '@/types/models'

defineProps<{
  visible: boolean
  editing: boolean
  saving: boolean
  form: GroupForm
  allAgents: AgentBrief[]
}>()

defineEmits<{ save: [] }>()

function getAgentTagType(status: string): string {
  const map: Record<string, string> = { RUNNING: 'success', EXECUTING: 'primary', STOPPED: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}

function getAgentStatusText(status: string): string {
  const map: Record<string, string> = { RUNNING: '运行中', EXECUTING: '执行中', STOPPED: '已停止', ERROR: '错误' }
  return map[status] || status
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.agent-selector {
  max-height: 200px; overflow-y: auto; border: 1px solid $border-subtle;
  border-radius: $radius-md; padding: 8px; background: $bg-surface;
}

.agent-checkbox-item { padding: 6px 0; }
.agent-checkbox-label { display: flex; align-items: center; gap: 8px; }
</style>
```

- [ ] **Step 7: 创建 useChatWebSocket.ts**

创建 `cat-web/src/views/groupChat/composables/useChatWebSocket.ts`：

```typescript
import { ref, onUnmounted } from 'vue'
import { cliWebSocket } from '@/utils/websocket'
import type { ChatMessage } from '@/types/models'

export function useChatWebSocket(groupId: string, onMessage: (msg: ChatMessage) => void, onAgentOutput: (data: any) => void) {
  const wsConnected = ref(false)

  async function connect() {
    try {
      await cliWebSocket.connect()
      wsConnected.value = true
      subscribe()
    } catch (error) {
      console.error('WebSocket connection failed:', error)
    }
  }

  function subscribe() {
    cliWebSocket.unsubscribeGroup(groupId)

    cliWebSocket.subscribeGroupMessage(groupId, (data: ChatMessage) => {
      onMessage(data)
    })

    cliWebSocket.subscribeGroupAgentOutput(groupId, (data: any) => {
      onAgentOutput(data)
    })
  }

  function disconnect() {
    if (groupId) {
      cliWebSocket.unsubscribeGroup(groupId)
    }
    wsConnected.value = false
  }

  onUnmounted(() => {
    disconnect()
  })

  return { wsConnected, connect, disconnect, subscribe }
}
```

- [ ] **Step 8: 重写精简后的 GroupChatView.vue**

用拆分后的组件重写 `cat-web/src/views/groupChat/GroupChatView.vue`。主视图现在只负责：
- 数据获取 (loadGroups, loadAllAgents, loadMessages)
- 状态协调 (selectedGroup, messages, activeOutputs)
- 事件处理 (selectGroup, sendMessage, CRUD)

新文件约 250 行（vs 原 1268 行）：

```vue
<template>
  <div class="group-chat">
    <ChatSidebar
      :groups="groups"
      :selectedId="selectedGroup?.id"
      @select="selectGroup"
      @create="openCreateDialog"
      @action="handleGroupAction"
    />

    <div class="chat-main">
      <template v-if="selectedGroup">
        <div class="chat-header">
          <div class="header-info">
            <span class="group-avatar-large">👥</span>
            <div class="header-text">
              <h2>{{ selectedGroup.name }}</h2>
              <p>{{ selectedGroup.description || '多Agent群聊' }}</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="agent-tags">
              <el-tag
                v-for="agent in selectedGroup.agents" :key="agent.id"
                size="small" :type="getAgentTagType(agent.status)" class="agent-tag"
              >🤖 {{ agent.name }}</el-tag>
            </div>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>

        <ChatMessageList
          :messages="messages"
          :activeOutputs="activeOutputs"
          :spinnerFrame="spinnerFrame"
          :agents="selectedGroup.agents || []"
        />

        <ChatInput
          :agents="selectedGroup.agents || []"
          :mentionedAgentIds="mentionedAgentIds"
          :isSending="isSending"
          @send="sendMessage"
          @toggleMention="toggleMention"
          @removeMention="removeMention"
          @clearMentions="mentionedAgentIds = []"
          @selectMentionFromInput="selectMentionFromInput"
        />
      </template>

      <div v-else class="no-group-selected">
        <div class="empty-icon">👈</div>
        <h2>选择一个群组开始聊天</h2>
        <p>从左侧选择群组，或创建一个新群组</p>
      </div>
    </div>

    <GroupCreateDialog
      :visible="showCreateDialog"
      :editing="!!editingGroup"
      :saving="savingGroup"
      :form="groupForm"
      :allAgents="allAgents"
      @save="handleSaveGroup"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listChatGroups, createChatGroup, updateChatGroup, deleteChatGroup, sendGroupMessage, getGroupMessages, clearGroupMessages } from '@/api/chatGroup'
import { getAgents } from '@/api/cliAgent'
import ChatSidebar from './components/ChatSidebar.vue'
import ChatMessageList from './components/ChatMessageList.vue'
import ChatInput from './components/ChatInput.vue'
import GroupCreateDialog from './components/GroupCreateDialog.vue'
import { useChatWebSocket } from './composables/useChatWebSocket'
import type { ChatGroup, ChatMessage, GroupForm, AgentBrief } from '@/types/models'

const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const messages = ref<ChatMessage[]>([])
const isSending = ref(false)
const allAgents = ref<AgentBrief[]>([])
const activeOutputs = ref<Record<string, string>>({})
const mentionedAgentIds = ref<string[]>([])

const showCreateDialog = ref(false)
const editingGroup = ref<ChatGroup | null>(null)
const savingGroup = ref(false)
const groupForm = ref<GroupForm>({ name: '', description: '', agentIds: [] })

const spinnerFrame = ref('⠋')
const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
let spinnerInterval: ReturnType<typeof setInterval> | null = null

const { wsConnected, connect } = useChatWebSocket(
  selectedGroup.value?.id || '',
  (msg) => {
    if (!messages.value.find(m => m.id === msg.id)) {
      messages.value.push(msg)
    }
  },
  (data) => {
    const { agentId, type, content } = data
    if (type === 'text_delta' && content) {
      if (!activeOutputs.value[agentId]) { activeOutputs.value[agentId] = ''; startSpinner() }
      activeOutputs.value[agentId] += content
    } else if (type === 'done') {
      delete activeOutputs.value[agentId]
      if (Object.keys(activeOutputs.value).length === 0) stopSpinner()
      if (selectedGroup.value) loadGroupMessages(selectedGroup.value.id)
    } else if (type === 'error') {
      delete activeOutputs.value[agentId]
      if (Object.keys(activeOutputs.value).length === 0) stopSpinner()
      if (selectedGroup.value) loadGroupMessages(selectedGroup.value.id)
    }
  }
)

watch(selectedGroup, (newGroup, oldGroup) => {
  if (oldGroup) activeOutputs.value = {}
  mentionedAgentIds.value = []
})

onMounted(async () => {
  await Promise.all([loadGroups(), loadAllAgents()])
  await connect()
})

async function loadGroups() {
  try { groups.value = await listChatGroups() || [] } catch (error) { console.error('加载群组失败:', error) }
}

async function loadAllAgents() {
  try {
    const result = await getAgents({ page: 1, pageSize: 100 })
    allAgents.value = (result.items || []).map((a: any) => ({ id: a.id, name: a.name, status: a.status }))
  } catch (error) { console.error('加载Agent失败:', error) }
}

async function loadGroupMessages(groupId: string) {
  try { messages.value = await getGroupMessages(groupId) || [] } catch (error) { messages.value = [] }
}

async function selectGroup(group: ChatGroup) {
  selectedGroup.value = group
  await loadGroupMessages(group.id)
}

async function sendMessage() { /* 保持原有逻辑 */ }
function toggleMention(agentId: string) { /* 保持原有逻辑 */ }
function removeMention(agentId: string) { /* 保持原有逻辑 */ }
function selectMentionFromInput(agentId: string) { /* 保持原有逻辑 */ }

function openCreateDialog() {
  editingGroup.value = null
  groupForm.value = { name: '', description: '', agentIds: [] }
  showCreateDialog.value = true
}

async function handleSaveGroup() { /* 保持原有逻辑 */ }
function handleGroupAction(command: string, group: ChatGroup) { /* 保持原有逻辑 */ }
async function clearChat() { /* 保持原有逻辑 */ }

function startSpinner() { /* 保持原有逻辑 */ }
function stopSpinner() { /* 保持原有逻辑 */ }

function getAgentTagType(status: string): string {
  const map: Record<string, string> = { RUNNING: 'success', EXECUTING: 'primary', STOPPED: 'info', ERROR: 'danger' }
  return map[status] || 'info'
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.group-chat {
  display: flex; height: calc(100vh - 64px - 48px);
  background: $bg-surface; border-radius: $radius-lg; overflow: hidden; border: 1px solid $border-subtle;
}

.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; background: $bg-base; }

.chat-header {
  padding: 12px 20px; border-bottom: 1px solid $border-subtle;
  display: flex; justify-content: space-between; align-items: center;
  background: $bg-surface; flex-wrap: wrap; gap: 8px;
}
.header-info { display: flex; align-items: center; gap: 12px; }
.group-avatar-large {
  font-size: 28px; width: 40px; height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md; display: flex; align-items: center; justify-content: center;
}
.header-text h2 { margin: 0; font-size: 16px; color: $text-primary; }
.header-text p { margin: 2px 0 0; font-size: 12px; color: $text-muted; }
.header-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.agent-tags { display: flex; gap: 4px; flex-wrap: wrap; }
.agent-tag { font-size: 11px; }

.no-group-selected {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: $text-muted; background: $bg-base;
  .empty-icon { font-size: 64px; margin-bottom: 20px; opacity: 0.4; }
  h2 { margin: 0 0 8px; color: $text-primary; }
}
</style>
```

- [ ] **Step 9: 验证前端编译**

```bash
cd cat-web
npm run build
```

Expected: 无错误输出

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "refactor: split GroupChatView into focused components

- 提取 ChatSidebar, ChatMessageList, ChatInput, MentionPopup, GroupCreateDialog
- 提取 useChatWebSocket composable
- 创建共享类型定义 types/models.ts
- GroupChatView 从 1268 行缩减到 ~250 行

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 前端组件拆分 — CLI Agent 模块

**Files:**
- Create: `cat-web/src/views/cliAgent/CliAgentCreateDialog.vue`
- Create: `cat-web/src/views/cliAgent/CliAgentEditDialog.vue`
- Create: `cat-web/src/composables/useSpinner.ts`
- Create: `cat-web/src/composables/useAgentPolling.ts`
- Modify: `cat-web/src/views/cliAgent/CliAgentListView.vue`
- Modify: `cat-web/src/views/cliAgent/CliAgentDetailView.vue`

- [ ] **Step 1: 创建 useSpinner.ts composable**

```typescript
// cat-web/src/composables/useSpinner.ts
import { ref, onUnmounted } from 'vue'

const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']

export function useSpinner() {
  const spinnerFrame = ref('⠋')
  let interval: ReturnType<typeof setInterval> | null = null

  function start() {
    if (interval) return
    let i = 0
    interval = setInterval(() => {
      spinnerFrame.value = spinnerFrames[i % spinnerFrames.length]
      i++
    }, 80)
  }

  function stop() {
    if (interval) { clearInterval(interval); interval = null }
  }

  onUnmounted(stop)

  return { spinnerFrame, startSpinner: start, stopSpinner: stop }
}
```

- [ ] **Step 2: 创建 useAgentPolling.ts composable**

```typescript
// cat-web/src/composables/useAgentPolling.ts
import { ref, onUnmounted } from 'vue'
import { getAgentStatus, getAgentTokenStats } from '@/api/cliAgent'

export function useAgentPolling(agentId: string, intervalMs: number = 5000) {
  const processStatus = ref<any>(null)
  const tokenStats = ref<any>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  async function loadProcessStatus() {
    try { processStatus.value = await getAgentStatus(agentId) } catch (e) { console.error(e) }
  }

  async function loadTokenStats() {
    try { tokenStats.value = await getAgentTokenStats(agentId) } catch (e) { console.error(e) }
  }

  function start() {
    loadProcessStatus()
    loadTokenStats()
    timer = setInterval(() => {
      loadProcessStatus()
      loadTokenStats()
    }, intervalMs)
  }

  function stop() {
    if (timer) { clearInterval(timer); timer = null }
  }

  onUnmounted(stop)

  return { processStatus, tokenStats, startPolling: start, stopPolling: stop }
}
```

- [ ] **Step 3: 创建 CliAgentCreateDialog.vue**

从 `CliAgentListView.vue` 中提取创建对话框部分（约 130 行）：

```vue
<!-- cat-web/src/views/cliAgent/CliAgentCreateDialog.vue -->
<template>
  <el-dialog v-model="visible" title="创建CLI Agent" width="600px" :close-on-click-modal="false">
    <!-- 表单内容和原 CliAgentListView 中创建对话框完全一致 -->
    <!-- 包括：模板选择、名称、描述、启动配置、环境变量、能力配置 -->
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTemplates, createAgent, getCapabilityTypes } from '@/api/cliAgent'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{ 'update:visible': [v: boolean]; created: [] }>()

const templates = ref<any[]>([])
const capabilityTypes = ref<any[]>([])
const creating = ref(false)
const envVarList = ref<{key: string, value: string}[]>([])

const form = reactive({
  templateId: '', name: '', description: '',
  executablePath: '', configPath: '', args: [] as string[],
  workingDir: '', capabilityType: '', domainTags: [] as string[],
  proficiencyLevel: 3
})

// ... 复制原 CliAgentListView 中创建相关的逻辑
// onTemplateChange, handleCreate, resetForm
</script>
```

**注意**：此步骤需要从 `CliAgentListView.vue` 精确复制创建对话框模板和 script 逻辑。实际代码太长，此处省略完整复制。关键是所有 import 和 ElMessage 等依赖保持不变。

- [ ] **Step 4: 创建 CliAgentEditDialog.vue**

同样从 `CliAgentListView.vue` 提取编辑对话框。结构与 Create 类似，增加 `editingAgent` prop。

- [ ] **Step 5: 精简 CliAgentListView.vue**

提取对话框后，原文件从 ~780 行缩减为 ~400 行（仅保留列表展示 + 搜索过滤 + 启动/停止/删除操作）。

- [ ] **Step 6: 更新 CliAgentDetailView.vue 使用 composables**

将 `startStatusPolling` / `stopStatusPolling` / `loadProcessStatus` / `loadTokenStats` 替换为 `useAgentPolling(agentId)` composable 调用。

将硬编码颜色替换为 SCSS 变量。将 `#1e1e1e` 替换为 `$bg-surface`，`#d4d4d4` 替换为 `$text-primary` 等。

- [ ] **Step 7: 验证编译并提交**

```bash
cd cat-web
npm run build
```

```bash
git add -A
git commit -m "refactor: split CliAgentListView dialogs and extract composables

- 提取 CliAgentCreateDialog 和 CliAgentEditDialog
- 创建 useSpinner 和 useAgentPolling composables
- CliAgentDetailView 硬编码颜色替换为 SCSS 变量

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: API 模块 TypeScript 化 + 依赖清理

**Files:**
- Create: `cat-web/src/api/cliAgent.ts`
- Create: `cat-web/src/api/chatGroup.ts`
- Delete: `cat-web/src/api/cliAgent.js`
- Delete: `cat-web/src/api/chatGroup.js`
- Modify: `cat-web/package.json`

- [ ] **Step 1: 转换 cliAgent.js → cliAgent.ts**

创建 `cat-web/src/api/cliAgent.ts`，基于原 `cliAgent.js` 但添加完整类型标注：

```typescript
import request from '@/utils/request'

// ===== 类型定义 =====

export interface CreateAgentPayload {
  name: string
  description?: string
  templateId: string
  executablePath?: string
  configPath?: string
  args?: string[]
  workingDir?: string
  envVars?: Record<string, string>
  capabilities?: Array<{
    type: string
    domainTags: string[]
    proficiencyLevel: number
  }>
}

export interface UpdateAgentPayload {
  name?: string
  description?: string
  executablePath?: string
  configPath?: string
  args?: string[]
  workingDir?: string
  envVars?: Record<string, string>
  capabilities?: Array<{
    type: string
    domainTags: string[]
    proficiencyLevel: number
  }>
}

export interface AgentQueryParams {
  page?: number
  pageSize?: number
  status?: string
  templateId?: string
  name?: string
}

// ===== 模板 API =====

export function getTemplates(): Promise<any[]> {
  return request.get('/cli-agent/templates')
}

export function getBuiltInTemplates(): Promise<any[]> {
  return request.get('/cli-agent/templates/built-in')
}

export function getTemplate(id: string): Promise<any> {
  return request.get(`/cli-agent/templates/${id}`)
}

export function createTemplate(data: any): Promise<any> {
  return request.post('/cli-agent/templates', data)
}

export function updateTemplate(id: string, data: any): Promise<any> {
  return request.put(`/cli-agent/templates/${id}`, data)
}

export function deleteTemplate(id: string): Promise<void> {
  return request.delete(`/cli-agent/templates/${id}`)
}

// ===== Agent 实例 API =====

export function getAgents(params: AgentQueryParams): Promise<{ items: any[]; total: number }> {
  return request.get('/cli-agents', { params })
}

export function getAgent(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}`)
}

export function createAgent(data: CreateAgentPayload): Promise<any> {
  return request.post('/cli-agents', data)
}

export function updateAgent(id: string, data: UpdateAgentPayload): Promise<any> {
  return request.put(`/cli-agents/${id}`, data)
}

export function deleteAgent(id: string): Promise<void> {
  return request.delete(`/cli-agents/${id}`)
}

export function getAvailableAgents(): Promise<any[]> {
  return request.get('/cli-agents/available')
}

// ===== 进程生命周期 API =====

export function startAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/start`)
}

export function stopAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/stop`)
}

export function restartAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/restart`)
}

export function getAgentStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/status`)
}

export function checkAgentHealth(id: string): Promise<boolean> {
  return request.get(`/cli-agents/${id}/health`)
}

// ===== 会话通信 API =====

export function sendInput(id: string, input: string): Promise<boolean> {
  return request.post(`/cli-agents/${id}/session/input`, input, {
    headers: { 'Content-Type': 'text/plain' }
  })
}

export function getSessionStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/session/status`)
}

export function closeSession(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/session/close`)
}

export function getOutputLogs(id: string, limit: number = 50): Promise<any[]> {
  return request.get(`/cli-agents/${id}/logs`, { params: { limit } })
}

export function clearOutputLogs(id: string): Promise<void> {
  return request.post(`/cli-agents/${id}/logs/clear`)
}

// ===== 任务执行 API =====

export function executeTask(id: string, data: { input: string; timeoutSeconds?: number }): Promise<any> {
  return request.post(`/cli-agents/${id}/tasks/execute`, data)
}

export function cancelTask(taskId: string): Promise<boolean> {
  return request.post(`/cli-agents/tasks/${taskId}/cancel`)
}

export function getTaskStatus(taskId: string): Promise<any> {
  return request.get(`/cli-agents/tasks/${taskId}/status`)
}

// ===== Token 统计 API =====

export function getAgentTokenStats(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/token-stats`)
}

export function getSystemTokenStats(params?: { startTime?: string; endTime?: string }): Promise<any> {
  return request.get('/cli-agents/system/token-stats', { params })
}

// ===== 监控 API =====

export function getAgentMonitorStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/monitor/${id}`)
}

export function getSystemOverview(): Promise<any> {
  return request.get('/cli-agents/monitor/overview')
}

// ===== 能力管理 API =====

export function getAgentCapabilities(id: string): Promise<any[]> {
  return request.get(`/cli-agents/${id}/capabilities`)
}

export function addCapability(id: string, data: any): Promise<any> {
  return request.post(`/cli-agents/${id}/capabilities`, data)
}

export function findAgentsByCapability(params: any): Promise<any[]> {
  return request.get('/cli-agents/by-capability', { params })
}

export function getCapabilityTypes(): Promise<any[]> {
  return request.get('/cli-agents/capability-types')
}

// ===== 消息通信 API =====

export function sendMessage(data: any): Promise<any> {
  return request.post('/cli-agents/messages/send', data)
}

export function getPendingMessages(id: string): Promise<any[]> {
  return request.get(`/cli-agents/messages/${id}/pending`)
}
```

- [ ] **Step 2: 转换 chatGroup.js → chatGroup.ts**

```typescript
import request from '@/utils/request'
import type { GroupForm, GroupMessagePayload } from '@/types/models'

export function createChatGroup(data: GroupForm): Promise<any> {
  return request.post('/chat-groups', data)
}

export function updateChatGroup(groupId: string, data: Partial<GroupForm>): Promise<any> {
  return request.put(`/chat-groups/${groupId}`, data)
}

export function deleteChatGroup(groupId: string): Promise<void> {
  return request.delete(`/chat-groups/${groupId}`)
}

export function getChatGroup(groupId: string): Promise<any> {
  return request.get(`/chat-groups/${groupId}`)
}

export function listChatGroups(): Promise<any[]> {
  return request.get('/chat-groups')
}

export function sendGroupMessage(groupId: string, data: GroupMessagePayload): Promise<any> {
  return request.post(`/chat-groups/${groupId}/messages`, data)
}

export function getGroupMessages(groupId: string, limit: number = 100): Promise<any[]> {
  return request.get(`/chat-groups/${groupId}/messages`, { params: { limit } })
}

export function clearGroupMessages(groupId: string): Promise<void> {
  return request.post(`/chat-groups/${groupId}/messages/clear`)
}
```

- [ ] **Step 3: 删除旧的 .js 文件**

```bash
rm cat-web/src/api/cliAgent.js
rm cat-web/src/api/chatGroup.js
```

- [ ] **Step 4: 移除未使用依赖**

在 `cat-web/package.json` 中删除 `echarts` 和 `vue-echarts`，删除 `sockjs-client`（Element Plus 自带的消息提示足够）。

```bash
cd cat-web
npm uninstall echarts vue-echarts
```

- [ ] **Step 5: 更新所有 import 路径**

确保所有文件中 `from '@/api/cliAgent'` 和 `from '@/api/chatGroup'` 的 import 在新 `.ts` 文件下正常工作。

- [ ] **Step 6: 验证编译并提交**

```bash
cd cat-web
npm run build
```

```bash
git add -A
git commit -m "refactor: convert API modules to TypeScript, remove unused deps

- cliAgent.js → cliAgent.ts (完整类型标注)
- chatGroup.js → chatGroup.ts (完整类型标注)
- 移除未使用依赖 echarts, vue-echarts

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 后端核心模块测试

**Files:**
- Create: `cat-standalone/src/test/java/com/cat/cliagent/CliAgentServiceTest.java`
- Create: `cat-standalone/src/test/java/com/cat/cliagent/CliProcessServiceTest.java`
- Create: `cat-standalone/src/test/java/com/cat/chatgroup/ChatGroupServiceTest.java`

- [ ] **Step 1: 编写 CliAgentService 测试**

```java
package com.cat.cliagent;

import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredCliAgent;
import com.cat.common.exception.BusinessException;
import com.cat.cliagent.dto.CliAgentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CliAgentServiceTest {

    private JsonFileStore<StoredCliAgent> store;
    private CliAgentService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        store = new JsonFileStore<>(tempDir.toString(), "test_cli_agents", StoredCliAgent.class);
        service = new CliAgentService(store);
    }

    @Test
    void createAgent_shouldSaveAndReturnAgent() {
        var agent = service.createAgent("Test Agent", "template-1", "system");

        assertNotNull(agent.getId());
        assertEquals("Test Agent", agent.getName());

        Optional<StoredCliAgent> stored = store.findById(agent.getId());
        assertTrue(stored.isPresent());
    }

    @Test
    void getAgent_shouldReturnExistingAgent() {
        var created = service.createAgent("Test", "tpl-1", "user-1");
        var found = service.getAgent(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Test", found.getName());
    }

    @Test
    void getAgent_shouldThrowForNonExistent() {
        assertThrows(BusinessException.class, () -> service.getAgent("nonexistent"));
    }

    @Test
    void deleteAgent_shouldRemoveAgent() {
        var agent = service.createAgent("ToDelete", "tpl-1", "user-1");
        service.deleteAgent(agent.getId());

        assertFalse(store.findById(agent.getId()).isPresent());
    }

    @Test
    void listAgents_shouldReturnPagedResults() {
        service.createAgent("A1", "tpl-1", "user-1");
        service.createAgent("A2", "tpl-1", "user-1");

        var page = service.listAgents(1, 10, null, null, null);
        assertEquals(2, page.getTotal());
    }
}
```

**注意**：实际测试代码需根据合并后的 CliAgentService 方法签名调整。

- [ ] **Step 2: 运行测试验证通过**

```bash
cd cat-standalone
mvn test -pl . -Dtest=CliAgentServiceTest
```

Expected: Tests run: 5, Failures: 0

- [ ] **Step 3: 编写 CliProcessService 测试**

测试启动/停止/重启流程，包括状态转换正确性和异常情况：

```java
// cat-standalone/src/test/java/com/cat/cliagent/CliProcessServiceTest.java
// 覆盖: startProcess (success/RUNNING拒绝/可执行文件不存在)
//        stopProcess (success/已STOPPED拒绝)
//        restartProcess (完整生命周期)
//        getProcessStatus (各状态)
//        isProcessHealthy (RUNNING/ERROR)
//        resetAgentStatusOnStartup (启动后重置RUNNING→STOPPED)
```

- [ ] **Step 4: 运行测试验证通过**

```bash
mvn test -pl . -Dtest=CliProcessServiceTest
```

- [ ] **Step 5: 编写 ChatGroupService 测试**

覆盖群组 CRUD + 消息发送流程：

```java
// cat-standalone/src/test/java/com/cat/chatgroup/ChatGroupServiceTest.java
// 覆盖: createGroup / getGroup / listGroups / updateGroup / deleteGroup
//        sendUserMessage (广播/@提及)
//        getGroupMessages / clearGroupMessages
```

- [ ] **Step 6: 运行全部测试**

```bash
mvn test
```

Expected: 所有测试通过

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "test: add unit tests for CliAgentService, CliProcessService, ChatGroupService

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: 清理与验证

**Files:**
- Delete: 空 `composables/` 目录（如果还有其他空目录）
- Modify: `cat-standalone/pom.xml` (更新 main class 引用)
- Modify: `run-standalone.bat` / `run-standalone.sh` (如果需要)

- [ ] **Step 1: 更新启动脚本中的 main class 引用**

```bash
# 检查 run-standalone.bat 是否有 CatStandaloneApplication 引用，改为 CatApplication
```

- [ ] **Step 2: 完整构建验证**

```bash
cd cat-standalone
mvn clean package -DskipTests

cd ../cat-web
npm run build
```

- [ ] **Step 3: 启动验证**

```bash
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar
```

验证：
- http://localhost:8080/api/v1/cli-agents 返回正常
- http://localhost:3000 前端页面正常加载
- Dashboard / CLI Agent / 群聊三个页面可访问

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "chore: update build scripts, verify full build passes

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Plan Summary

| Task | 内容 | 预估时间 |
|------|------|---------|
| 1 | 删除后端废弃模块 | 15 min |
| 2 | 删除前端废弃模块 + 路由精简 | 15 min |
| 3 | 后端包结构重组 | 30 min |
| 4 | 前端群聊组件拆分 | 30 min |
| 5 | 前端 CLI Agent 组件拆分 + composables | 20 min |
| 6 | API TypeScript 化 + 依赖清理 | 15 min |
| 7 | 后端核心测试 | 30 min |
| 8 | 清理验证 | 15 min |
| **Total** | | ~3 hours |
