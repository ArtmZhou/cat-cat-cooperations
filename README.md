# 猫猫多Agent协同系统 (Cat Agent Platform)

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4-4FC08D.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 一个支持多Agent协同工作的CLI Agent工具对接平台，提供实时通信和状态监控。

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [开发指南](#开发指南)
- [API文档](#api文档)
- [贡献指南](#贡献指南)

---

## 项目简介

猫猫多Agent协同系统（Cat Agent Platform）是一个多智能体协同工作平台，支持对接外部CLI Agent工具（如Claude Code、OpenCode等），实现Agent管理、群聊协作和实时监控。

### 主要特点

- **CLI Agent对接**：支持Claude Code、OpenCode等CLI工具的集成和管理
- **多Agent群聊**：支持创建群组，@提及指定Agent或广播消息
- **实时通信**：基于WebSocket的实时输出推送和消息通信
- **深色科技风UI**：紫蓝渐变主题，毛玻璃效果，定制SVG图标系统
- **简化部署**：无需数据库，无需认证，一键启动

---

## 核心功能

### 1. CLI Agent管理
- **模板管理**：内置Claude Code、OpenCode配置模板
- **实例管理**：基于模板创建Agent实例，配置启动参数和环境变量
- **进程管理**：CLI进程启动、停止、重启
- **能力管理**：Agent能力类型和领域标签声明与匹配

### 2. 多Agent群聊
- **群组管理**：创建/编辑/删除聊天群组
- **消息发送**：支持广播给所有Agent，或@提及指定Agent
- **上下文感知**：Agent能感知群聊中其他参与者的消息
- **流式输出**：Agent回复实时推送到群聊界面

### 3. 监控与统计
- **系统仪表盘**：Agent数量、执行状态、Token使用统计
- **WebSocket推送**：CLI输出实时推送到前端
- **Token统计**：从CLI输出解析Token使用量

### 4. 前端界面
- **深色科技风主题**：紫蓝渐变（Violet #7C3AED → Cyan #06B6D4）配色
- 12个定制SVG图标组件（猫猫、仪表盘、终端、消息等）
- 毛玻璃（Glassmorphism）顶部导航栏
- 渐变色侧边栏，动态激活指示器
- 群聊暗色主题，渐变消息气泡和流式输出动画

---

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                           前端层                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Vue 3 + TypeScript + Element Plus + Vite              │   │
│  │  Port: 3000                                             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          后端层                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Spring Boot 3.2 + Java 17                              │   │
│  │  Port: 8080                                             │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Controller → Service → JsonFileStore (flat)            │   │
│  │  • CLI Agent 管理 (CRUD / 进程 / 会话 / 任务)           │   │
│  │  • 群聊管理 (群组 / 消息 / @提及 / 输出路由)           │   │
│  │  • WebSocket 实时推送 (STOMP over SockJS)              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          数据层                                 │
│  ┌──────────────────────┐  ┌──────────────────────┐           │
│  │  JSON File Store     │  │  Embedded Redis      │           │
│  │  ./data/*.json       │  │  Port: 6380 (可选)   │           │
│  └──────────────────────┘  └──────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 前端框架 | Vue | 3.4+ | Composition API |
| 前端UI | Element Plus | 2.5+ | 组件库（深色主题覆盖） |
| 前端构建 | Vite | 5.0+ | 构建工具 |
| 前端语言 | TypeScript | 5.0+ | 类型安全 |
| 后端框架 | Spring Boot | 3.2+ | Java框架 |
| 后端语言 | Java | 17 LTS | JDK版本 |
| 数据存储 | JSON File | - | 本地文件持久化 |
| 缓存 | Embedded Redis | 0.7.3 | 可选缓存 |
| WebSocket | STOMP over SockJS | - | 实时通信 |

---

## 项目结构

```
cat-cat-cooperations/
├── cat-standalone/              # 后端（单机模式）
│   └── src/main/java/com/cat/
│       ├── CatApplication.java  # 启动入口
│       ├── controller/          # REST控制器 (5个)
│       ├── cliagent/            # CLI Agent服务 + DTO (9个服务)
│       ├── chatgroup/           # 群聊服务 + 实体
│       ├── config/              # Spring配置 (Web/WS/Store/异常)
│       ├── store/               # JSON文件存储层
│       ├── common/              # 共享模型
│       └── dashboard/           # 仪表盘
├── cat-web/                     # Vue 3前端
│   └── src/
│       ├── api/                 # API模块 (TypeScript)
│       │   ├── cliAgent.ts
│       │   └── chatGroup.ts
│       ├── assets/styles/       # SCSS设计系统
│       │   ├── _variables.scss  # 设计令牌
│       │   └── main.scss        # 全局样式
│       ├── components/          # 共享组件
│       │   ├── CatIcons.ts      # 12个SVG图标
│       │   └── layout/          # AppLayout
│       ├── composables/         # 可复用逻辑
│       │   ├── useSpinner.ts
│       │   └── useAgentPolling.ts
│       ├── types/               # 共享类型定义
│       │   └── models.ts
│       ├── views/               # 页面组件
│       │   ├── dashboard/       # 仪表盘
│       │   ├── cliAgent/        # CLI Agent管理 + 对话框
│       │   └── groupChat/       # 群聊 + 5个子组件
│       ├── router/              # 路由配置 (4条路由)
│       └── utils/               # Axios封装 + WebSocket
├── docs/                        # 文档
├── run-standalone.sh            # Linux/Mac启动脚本
└── run-standalone.bat           # Windows启动脚本
```

---

## 快速开始

### 环境要求

- **Java**: JDK 17+
- **Maven**: 3.8+
- **Node.js**: 18+ (前端开发需要)

### 方式一：使用启动脚本（推荐）

```bash
# Linux/Mac
./run-standalone.sh

# Windows
run-standalone.bat
```

### 方式二：手动构建启动

```bash
# 1. 构建项目
mvn clean package -pl cat-standalone -am -DskipTests

# 2. 启动服务
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar
```

### 方式三：使用Maven直接运行

```bash
mvn spring-boot:run -pl cat-standalone
```

### 访问服务

启动成功后：

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端API | http://localhost:8080/api/v1 | RESTful API |
| 前端页面 | http://localhost:3000 | 需单独启动前端 |
| WebSocket | ws://localhost:8080/ws | 实时通信 |

### 前端启动

```bash
cd cat-web
npm install
npm run dev
```

---

## 开发指南

### 后端开发

```bash
# 完整构建
mvn clean package -DskipTests

# 仅构建单机模块
mvn clean package -pl cat-standalone -am -DskipTests

# 运行测试 (46个测试)
mvn test -pl cat-standalone
```

#### 数据存储

单机模式使用JSON文件存储，数据目录 `./data/`：

| 文件 | 说明 |
|------|------|
| `cli_agents.json` | CLI Agent实例数据 |
| `cli_agent_templates.json` | Agent模板数据 |
| `cli_agent_capabilities.json` | Agent能力数据 |
| `token_usage_logs.json` | Token使用记录 |
| `cli_agent_output_logs.json` | CLI输出日志 |
| `chat_groups.json` | 聊天群组数据 |
| `chat_group_messages.json` | 群聊消息数据 |

### 前端开发

```bash
cd cat-web

# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建
npm run build

# 代码检查
npm run lint
```

---

## API文档

### CLI Agent管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/cli-agents` | GET | 获取Agent列表 |
| `/api/v1/cli-agents` | POST | 创建Agent |
| `/api/v1/cli-agents/{id}` | GET | 获取Agent详情 |
| `/api/v1/cli-agents/{id}` | PUT | 更新Agent |
| `/api/v1/cli-agents/{id}` | DELETE | 删除Agent |
| `/api/v1/cli-agents/{id}/actions/start` | POST | 启动Agent |
| `/api/v1/cli-agents/{id}/actions/stop` | POST | 停止Agent |
| `/api/v1/cli-agents/{id}/actions/restart` | POST | 重启Agent |
| `/api/v1/cli-agents/{id}/session/input` | POST | 发送输入 |
| `/api/v1/cli-agents/{id}/logs` | GET | 获取输出日志 |
| `/api/v1/cli-agents/{id}/token-stats` | GET | Token统计 |
| `/api/v1/cli-agents/monitor/overview` | GET | 系统概览 |

### 群聊管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/chat-groups` | GET | 获取群组列表 |
| `/api/v1/chat-groups` | POST | 创建群组 |
| `/api/v1/chat-groups/{id}` | GET | 获取群组详情 |
| `/api/v1/chat-groups/{id}` | PUT | 更新群组 |
| `/api/v1/chat-groups/{id}` | DELETE | 删除群组 |
| `/api/v1/chat-groups/{id}/messages` | POST | 发送消息（支持@提及/广播） |
| `/api/v1/chat-groups/{id}/messages` | GET | 获取历史消息 |
| `/api/v1/chat-groups/{id}/messages/clear` | POST | 清空消息 |

### WebSocket

连接地址：`ws://localhost:8080/ws`

订阅主题：
- `/topic/cli/{agentId}/output` - Agent输出流
- `/topic/cli/status/{agentId}` - Agent状态变更
- `/topic/chat-group/{groupId}/message` - 群聊新消息
- `/topic/chat-group/{groupId}/agent-output` - 群聊Agent输出

---

## 扩展开发

### 添加新的CLI Agent模板

在 `com.cat.cliagent.CliAgentTemplateService` 中注册新模板：

```java
CliAgentTemplate template = new CliAgentTemplate();
template.setName("My Custom CLI");
template.setCliType("my-cli");
template.setExecutablePath("/usr/local/bin/my-cli");
// ... 保存模板
```

### 添加新的API接口

遵循 Controller → Service → Store 架构：

```java
// Controller in com.cat.controller
@RestController
@RequestMapping("/api/v1/custom")
@RequiredArgsConstructor
public class CustomController {
    private final CustomService customService;

    @GetMapping
    public ApiResponse<List<CustomEntity>> list() {
        return ApiResponse.success(customService.findAll());
    }
}
```

### 前端页面扩展

在 `cat-web/src/views/` 中添加新页面，在 `cat-web/src/router/index.ts` 中注册路由。

---

## 贡献指南

### 代码规范

- 后端：遵循Java编码规范
- 前端：ESLint + Vue 3风格指南

---

## 文档索引

- [CLAUDE.md](CLAUDE.md) - Claude Code开发指南
- [架构说明书](docs/architecture.md) - 详细架构设计（部分章节需更新）
- [重构设计](docs/superpowers/specs/2026-05-10-platform-refactor-design.md) - 最新重构设计规格

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

<p align="center">
  <img src="https://img.shields.io/badge/Made%20with-%F0%9F%90%B1-orange" alt="Made with cat">
</p>
