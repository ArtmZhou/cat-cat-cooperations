# Cat Agent Platform 全面重构设计

## 目标

系统性重构项目，删除死代码和未完成模块，聚焦3个核心子系统（CLI Agent、群聊、Dashboard），提升代码质量和测试覆盖率。

## 模块取舍

### 保留（3个核心子系统）

| 子系统 | 理由 |
|--------|------|
| CLI Agent | 平台核心，功能完整，前后端齐全 |
| 群聊 | 唯一的 Agent 协作入口，前后端齐全 |
| Dashboard | 系统概览，依赖 CLI Agent 数据 |

### 删除（6个模块 + 登录系统）

| 模块 | 删除理由 |
|------|----------|
| Task (后端) | 后端完整但前端为零，CLI Agent 已有 /tasks/execute 替代 |
| Agent 通用 (前后端) | 与 CLI Agent 概念重叠，无实际使用场景 |
| Agent 消息 (后端) | 独立的消息服务，群聊已覆盖此能力 |
| 用户管理 (前端) | 无后端 API 支撑 |
| 角色管理 (前端) | 无后端 API 支撑 |
| 登录系统 | 任意用户名密码均通过，无实际认证需求 |
| echarts 依赖 | 已安装但零使用 |

## 后端架构

```
com.cat/
├── common/                     # 跨模块共享
│   ├── ApiResponse.java
│   ├── PageResult.java
│   └── BusinessException.java
├── config/                     # 所有配置
│   ├── WebConfig.java          # CORS
│   ├── WebSocketConfig.java    # STOMP
│   ├── StoreConfig.java        # JsonFileStore Bean
│   └── GlobalExceptionHandler.java
├── cliagent/                   # CLI Agent 子系统
│   ├── CliAgentController.java
│   ├── CliAgentService.java
│   ├── CliProcessService.java
│   ├── CliSessionService.java
│   ├── CliTaskExecutionService.java
│   ├── CliOutputPushService.java
│   ├── TokenUsageService.java
│   ├── dto/
│   └── entity/
├── chatgroup/                  # 群聊子系统
│   ├── ChatGroupController.java
│   ├── ChatGroupService.java
│   └── entity/
├── dashboard/                  # 仪表盘
│   └── DashboardController.java
└── store/                      # 存储层
    ├── JsonFileStore.java
    └── StoreConfig.java
```

### 关键变化

- 消除接口/实现分离 — 仅一种实现时直接合为单一类
- Controller 直接注入 Service，去掉中间层
- 删除模块的 Entity/Store Bean 一并清理
- 包名去 `standalone` 前缀
- 补全核心模块测试

## 前端架构

```
cat-web/src/
├── api/
│   ├── cliAgent.ts            # JS→TS，完整类型
│   ├── chatGroup.ts           # JS→TS，完整类型
│   └── request.ts
├── components/
│   ├── CatIcons.vue
│   └── layout/
│       └── AppLayout.vue      # 去用户信息/登出
├── composables/
│   ├── useAgentPolling.ts
│   └── useSpinner.ts
├── views/
│   ├── dashboard/
│   │   └── DashboardView.vue
│   ├── cliAgent/
│   │   ├── CliAgentListView.vue
│   │   ├── CliAgentCreateDialog.vue
│   │   ├── CliAgentEditDialog.vue
│   │   └── CliAgentDetailView.vue
│   └── groupChat/
│       ├── GroupChatView.vue
│       └── components/
│           ├── ChatSidebar.vue
│           ├── ChatMessageList.vue
│           ├── ChatInput.vue
│           ├── MentionPopup.vue
│           └── GroupCreateDialog.vue
├── router/index.ts            # 精简至4条路由
├── assets/styles/
└── types/
    └── models.ts              # 共享类型定义
```

### 关键变化

- API 模块 TypeScript 化，完整类型标注
- GroupChatView 从 1260 行拆分为 6 个文件
- 提取 composables：轮询、spinner、WebSocket 逻辑
- 统一类型定义到 types/models.ts
- 组件中硬编码颜色全部改用 SCSS 变量
- 删除 composables 空目录（替换为实际内容）
- 路由精简为：Dashboard / CLI Agent List / CLI Agent Detail / 群聊

## 测试策略

- 后端核心 Service 类全部补单元测试（TDD 方式）
- 前端关键 composables 补 vitest 测试
- 目标：代码覆盖率从 ~5% 提升到 60%+

## 不做什么

- 不新增功能，只清理和优化现有代码
- 不改 API 语义（端点路径和响应格式保持不变）
- 不更换技术栈（保持 Vue 3 + Spring Boot + Element Plus）
