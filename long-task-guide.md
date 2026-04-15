# 猫猫多Agent协同系统 - 开发指南

## 项目概述

猫猫多Agent协同系统是一个支持CLI Agent工具（Claude Code、OpenCode等）对接的轻量级平台。系统采用独立模式(standalone)架构，零外部依赖，一键启动。

**核心特性**:
- CLI Agent模板管理（Claude Code / OpenCode）
- CLI Agent实例管理（配置、启动、监控）
- 实时对话（WebSocket流式输出）
- 任务执行控制（超时、取消）
- Token使用统计

## 技术栈

### 后端
- **框架**: Spring Boot 3.2+
- **语言**: Java 17
- **构建**: Maven
- **存储**: JSON文件（./data/目录）
- **缓存**: 嵌入式Redis（可选，端口6380）

### 前端
- **框架**: Vue 3.4+
- **语言**: TypeScript 5.0+
- **构建**: Vite 5.0+
- **UI库**: Element Plus 2.5+
- **状态管理**: Pinia

## 项目结构

```
cat-cat-cooperations/
├── cat-standalone/              # 后端核心模块
│   ├── src/main/java/com/cat/standalone/
│   │   ├── controller/          # REST API控制器
│   │   ├── service/             # 业务逻辑服务
│   │   │   ├── LocalCliAgentService.java
│   │   │   ├── LocalCliSessionService.java
│   │   │   ├── LocalCliProcessService.java
│   │   │   ├── LocalCliTaskExecutionService.java
│   │   │   └── LocalTokenUsageService.java
│   │   ├── store/               # JSON文件存储
│   │   │   ├── JsonFileStore.java
│   │   │   └── entity/          # 存储实体
│   │   ├── auth/                # 简化认证
│   │   └── config/              # 配置类
│   ├── src/main/java/com/cat/cliagent/
│   │   ├── dto/                 # 数据传输对象
│   │   └── service/             # 服务接口
│   ├── src/main/resources/
│   │   └── application.yml      # 应用配置
│   └── data/                    # JSON数据存储目录
│       ├── cli_agents.json
│       ├── cli_agent_templates.json
│       ├── tasks.json
│       └── ...
├── cat-web/                     # 前端应用
│   └── src/
│       ├── api/                 # API模块
│       │   ├── cliAgent.js      # CLI Agent API
│       │   └── task.js          # 任务API
│       ├── views/               # 页面组件
│       │   ├── groupChat/        # 群聊
│       │   ├── cliAgent/        # Agent管理
│       │   └── task/            # 任务管理
│       ├── utils/               # 工具类
│       │   └── websocket.js     # WebSocket客户端
│       └── stores/              # Pinia状态管理
├── docs/                        # 文档
│   └── plans/                   # 规划文档
├── pom.xml                      # Maven配置
├── run-standalone.bat           # Windows启动脚本
├── run-standalone.sh            # Linux/Mac启动脚本
└── CLAUDE.md                    # 项目指南
```

## 开发准则

- 每次实现一个新功能都要记录下来
- 每次修复一个问题都要记录下来
- 实现或修复的东西要有一个简略说明记录到`task-progress.md`文件中

## 开发流程

### 1. 快速启动

```bash
# 后端（端口8080）
mvn spring-boot:run -pl cat-standalone

# 前端（端口3000）
cd cat-web
npm install
npm run dev
```

**访问地址**:
- 前端: http://localhost:3000
- API: http://localhost:8080/api/v1
- WebSocket: ws://localhost:8080/ws

### 2. 代码规范

#### Java代码规范
- 使用Lombok简化代码
- 类和方法必须有Javadoc注释
- JSON存储实体放在`store/entity/`包下
- 服务实现类以`Local`前缀命名

#### 前端代码规范
- 使用Composition API (`<script setup>`)
- 组件命名采用PascalCase
- API模块放在`src/api/`目录
- 使用TypeScript类型定义

### 3. Git提交规范

```
<type>(<scope>): <subject>

<body>
```

**type类型**:
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建/工具

## 核心架构

### CLI Agent执行模式

**Per-Request模式（--print）**:
- 每次请求启动独立的CLI进程
- 使用 `--resume <sessionId>` 恢复对话上下文
- 解析 `stream-json` 格式输出
- WebSocket实时推送text_delta和done消息

**核心服务**:
- `LocalCliAgentService` - Agent CRUD
- `LocalCliSessionService` - 会话管理（每请求执行）
- `LocalCliProcessService` - 进程生命周期（简化版）
- `LocalCliTaskExecutionService` - 任务执行
- `LocalTokenUsageService` - Token统计
- `LocalCliOutputPushService` - WebSocket推送

### 数据存储

**JSON文件（./data/目录）**:
- `cli_agents.json` - Agent实例
- `cli_agent_templates.json` - 模板配置
- `cli_agent_capabilities.json` - 能力配置
- `tasks.json` - 任务数据
- `task_assignments.json` - 任务分配
- `task_logs.json` - 执行日志
- `token_usage_logs.json` - Token使用记录

## 测试策略

### 单元测试
- 使用JUnit 5 + Mockito
- 运行: `mvn test -pl cat-standalone`

### 集成测试
- 使用Spring Boot Test
- 运行: `mvn verify -pl cat-standalone`

### 前端测试
- 使用Vitest（可选）
- 运行: `npm run test`

## 关键API端点

### CLI Agent管理
- `GET /api/v1/cli-agents` - 列表
- `POST /api/v1/cli-agents` - 创建
- `GET /api/v1/cli-agents/{id}` - 详情
- `POST /api/v1/cli-agents/{id}/actions/start` - 启动
- `POST /api/v1/cli-agents/{id}/actions/stop` - 停止

### CLI Agent通信
- `POST /api/v1/cli-agents/{id}/session/input` - 发送输入
- `GET /api/v1/cli-agents/{id}/logs` - 获取日志
- WebSocket `/ws` - 实时输出

### 任务管理
- `GET /api/v1/tasks` - 列表
- `POST /api/v1/tasks` - 创建
- `POST /api/v1/tasks/{id}/cancel` - 取消

## 常见问题

### Q: 如何添加新的CLI Agent模板?
A: 在`StoredCliAgentTemplate`中添加模板配置，内置模板在`LocalCliAgentTemplateService`中初始化。

### Q: 如何扩展前端页面?
A: 在`cat-web/src/views/`创建新组件，在`cat-web/src/router/index.ts`添加路由。

### Q: 数据存储在哪里?
A: 所有数据存储在`cat-standalone/data/`目录下的JSON文件中，可直接查看和备份。

## 联系方式

- 项目负责人: TBD
- 技术支持: TBD
