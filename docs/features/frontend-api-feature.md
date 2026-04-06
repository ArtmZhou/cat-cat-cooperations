# 前端API集成文档

**特性编号:** FEAT-025 至 FEAT-030
**模块名称:** cat-web
**实现日期:** 2026-04-02
**状态:** 已完成

---

## 1. 概述

完成前端与后端API的集成，将页面从mock数据切换到真实API调用。实现：
- 统一的API请求封装
- Agent管理API集成
- 任务管理API集成
- 监控API集成
- 用户管理API集成

---

## 2. API模块结构

```
cat-web/src/api/
├── auth.ts      # 认证相关API
├── agent.ts     # Agent管理API
├── task.ts      # 任务管理API
├── monitor.ts   # 监控相关API
└── user.ts      # 用户管理API
```

---

## 3. 请求工具

### 3.1 request.ts

位置: `src/utils/request.ts`

基于Axios封装，提供：
- 自动添加Token到请求头
- 响应拦截处理错误
- 401自动跳转登录
- 统一的错误提示

### 3.2 使用方式

```typescript
import { request } from '@/utils/request'

// GET请求
const data = await request.get('/api/endpoint')

// POST请求
const result = await request.post('/api/endpoint', { data })

// PUT请求
await request.put('/api/endpoint', { data })

// DELETE请求
await request.delete('/api/endpoint')
```

---

## 4. API模块详情

### 4.1 Agent API (agent.ts)

| 函数 | 方法 | 接口 | 描述 |
|------|------|------|------|
| getAgentList | GET | /agents | 获取Agent列表 |
| getAgent | GET | /agents/:id | 获取Agent详情 |
| createAgent | POST | /agents | 创建Agent |
| updateAgent | PUT | /agents/:id | 更新Agent |
| deleteAgent | DELETE | /agents/:id | 删除Agent |
| enableAgent | PUT | /agents/:id/enable | 启用Agent |
| disableAgent | PUT | /agents/:id/disable | 禁用Agent |
| getAgentCapabilities | GET | /agents/:id/capabilities | 获取能力列表 |
| registerCapability | POST | /agents/:id/capabilities | 注册能力 |
| generateAccessKey | POST | /agents/:id/access-key | 生成接入凭证 |

### 4.2 Task API (task.ts)

| 函数 | 方法 | 接口 | 描述 |
|------|------|------|------|
| getTaskList | GET | /tasks | 获取任务列表 |
| getTask | GET | /tasks/:id | 获取任务详情 |
| createTask | POST | /tasks | 创建任务 |
| updateTask | PUT | /tasks/:id | 更新任务 |
| deleteTask | DELETE | /tasks/:id | 删除任务 |
| cancelTask | PUT | /tasks/:id/cancel | 取消任务 |
| retryTask | PUT | /tasks/:id/retry | 重试任务 |
| getTaskAssignments | GET | /tasks/:id/assignments | 获取任务分配 |
| getTaskLogs | GET | /tasks/:id/logs | 获取任务日志 |
| getTaskStatistics | GET | /tasks/statistics | 获取任务统计 |
| scheduleTask | POST | /tasks/:id/schedule | 创建调度 |
| cancelSchedule | DELETE | /tasks/:id/schedule | 取消调度 |

### 4.3 Monitor API (monitor.ts)

| 函数 | 方法 | 接口 | 描述 |
|------|------|------|------|
| getCurrentMetrics | GET | /monitor/metrics | 获取当前指标 |
| getMetricsHistory | GET | /monitor/metrics/history | 获取历史指标 |
| getTaskStatistics | GET | /monitor/tasks/statistics | 获取任务统计 |
| getAgentStatusDistribution | GET | /monitor/agents/distribution | Agent状态分布 |
| getAlertRules | GET | /alerts/rules | 获取告警规则 |
| createAlertRule | POST | /alerts/rules | 创建告警规则 |
| getAlerts | GET | /alerts | 获取告警列表 |
| acknowledgeAlert | PUT | /alerts/:id/acknowledge | 确认告警 |
| resolveAlert | PUT | /alerts/:id/resolve | 解决告警 |
| getActiveAlertCounts | GET | /alerts/counts | 获取告警统计 |

### 4.4 User API (user.ts)

| 函数 | 方法 | 接口 | 描述 |
|------|------|------|------|
| getUserList | GET | /users | 获取用户列表 |
| getUser | GET | /users/:id | 获取用户详情 |
| createUser | POST | /users | 创建用户 |
| updateUser | PUT | /users/:id | 更新用户 |
| deleteUser | DELETE | /users/:id | 删除用户 |
| resetUserPassword | PUT | /users/:id/password | 重置密码 |
| enableUser | PUT | /users/:id/enable | 启用用户 |
| disableUser | PUT | /users/:id/disable | 禁用用户 |
| getRoleList | GET | /roles | 获取角色列表 |
| createRole | POST | /roles | 创建角色 |
| getAllPermissions | GET | /permissions | 获取所有权限 |

---

## 5. 类型定义

每个API模块都导出完整的TypeScript类型定义：

### 5.1 Agent类型

```typescript
export interface Agent {
  id: string
  name: string
  description?: string
  type: 'BUILT_IN' | 'EXTERNAL'
  status: 'OFFLINE' | 'ONLINE' | 'BUSY' | 'ERROR' | 'DISABLED'
  // ...
}

export interface CreateAgentRequest {
  name: string
  description?: string
  type: 'BUILT_IN' | 'EXTERNAL'
  // ...
}
```

### 5.2 Task类型

```typescript
export type TaskType = 'SIMPLE' | 'WORKFLOW' | 'PARALLEL' | 'NEGOTIATION'
export type TaskStatus = 'PENDING' | 'ASSIGNED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
export type TaskPriority = 0 | 1 | 2

export interface Task {
  id: string
  name: string
  type: TaskType
  status: TaskStatus
  // ...
}
```

---

## 6. 页面集成状态

| 页面 | 组件 | 状态 |
|------|------|------|
| 登录页 | LoginView.vue | 已集成auth API |
| 仪表盘 | DashboardView.vue | 待集成monitor API |
| Agent列表 | AgentListView.vue | 已集成agent API |
| Agent详情 | AgentDetailView.vue | 待集成 |
| 任务列表 | TaskListView.vue | 已集成task API |
| 任务详情 | TaskDetailView.vue | 待集成 |
| 监控页 | MonitorView.vue | 待集成monitor API |
| 用户管理 | UserManagementView.vue | 待集成user API |
| 角色管理 | RoleManagementView.vue | 待集成user API |

---

## 7. 环境配置

### 7.1 开发环境

`.env.development`:
```
VITE_API_BASE_URL=/api/v1
```

### 7.2 生产环境

`.env.production`:
```
VITE_API_BASE_URL=/api/v1
```

---

## 8. 使用示例

### 8.1 在组件中使用API

```typescript
import { ref, onMounted } from 'vue'
import { getAgentList, type Agent } from '@/api/agent'

const agents = ref<Agent[]>([])
const loading = ref(false)

async function loadAgents() {
  loading.value = true
  try {
    const result = await getAgentList({ page: 1, size: 10 })
    agents.value = result.records
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAgents()
})
```

### 8.2 创建数据

```typescript
import { createTask } from '@/api/task'
import { ElMessage } from 'element-plus'

async function handleCreate() {
  try {
    await createTask({
      name: '新任务',
      type: 'SIMPLE',
      priority: 1
    })
    ElMessage.success('创建成功')
  } catch (error) {
    // 错误已在request中统一处理
  }
}
```

---

## 9. 特性验收状态

| 验收标准 | 状态 |
|----------|------|
| 前端API模块创建 | ✓ 已创建auth/agent/task/monitor/user |
| 类型定义完整 | ✓ 完整TypeScript类型 |
| Agent列表页集成 | ✓ 已完成 |
| 任务列表页集成 | ✓ 已完成 |
| 统一错误处理 | ✓ request拦截器处理 |
| Token自动添加 | ✓ request拦截器处理 |

---

## 10. 后续工作

1. 完成仪表盘页面数据集成
2. 完成Agent详情页API集成
3. 完成任务详情页API集成
4. 完成监控页面API集成
5. 完成用户管理页面API集成
6. 添加WebSocket实时更新支持