# 猫猫多Agent协同系统 (Cat Agent Platform)

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4-4FC08D.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 一个支持多Agent协同工作的CLI Agent工具对接平台，提供任务编排、实时通信和状态监控等核心能力。

## 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [开发指南](#开发指南)
- [扩展开发](#扩展开发)
- [API文档](#api文档)
- [贡献指南](#贡献指南)

---

## 项目简介

猫猫多Agent协同系统（Cat Agent Platform）是一个多智能体协同工作平台，支持对接外部CLI Agent工具（如Claude Code、OpenCode等），实现任务的自动化编排、执行和监控。

### 主要特点

- **CLI Agent对接**：支持Claude Code、OpenCode等CLI工具的集成和管理
- **多Agent协同**：支持流程化任务、并行任务、协商决策等多种协同模式
- **实时通信**：基于WebSocket的实时输出推送和消息通信
- **可视化监控**：猫猫主题风格的Web界面，实时监控Agent状态和任务进度
- **简化部署**：单机模式无需数据库，一键启动，适合本地开发和测试

---

## 核心功能

### 1. 用户认证与权限管理
- JWT Token认证机制
- RBAC角色权限控制
- 用户、角色、权限管理

### 2. CLI Agent管理
- **模板管理**：内置Claude Code、OpenCode配置模板
- **实例管理**：基于模板创建Agent实例，配置启动参数和环境变量
- **进程管理**：CLI进程启动、监控、终止、异常重启
- **能力管理**：Agent能力类型和领域标签声明与匹配

### 3. 任务管理
- **任务类型**：
  - 简单任务：单Agent执行的独立任务
  - 流程任务：多步骤顺序执行任务
  - 并行任务：多Agent并行执行任务
  - 协商任务：Agent投票协商决策任务
- **输入方式**：支持自然语言Prompt、JSON参数、文件路径
- **调度方式**：立即执行、定时执行、周期执行
- **状态追踪**：实时更新任务状态和执行进度

### 4. 通信与监控
- **持久会话**：stdin/stdout持久会话，支持流式输出
- **WebSocket推送**：CLI输出实时推送到前端
- **Token统计**：从CLI输出解析Token使用量
- **系统监控**：系统资源、Agent状态、任务执行统计

### 5. 前端界面
- 猫猫主题风格设计
- 登录、仪表盘、Agent管理、任务管理页面
- 实时监控和日志查看
- 响应式布局，支持移动端

---

## 技术架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                           前端层                                │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Vue 3 + TypeScript + Element Plus + Pinia             │   │
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
│  │  • cat-agent: Agent管理服务                             │   │
│  │  • cat-task: 任务管理服务                               │   │
│  │  • cat-runtime: 运行时执行服务                          │   │
│  │  • cat-orchestration: 流程编排服务                      │   │
│  │  • cat-message: WebSocket消息服务                       │   │
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
| 前端UI | Element Plus | 2.5+ | 组件库 |
| 前端构建 | Vite | 5.0+ | 构建工具 |
| 前端语言 | TypeScript | 5.0+ | 类型安全 |
| 状态管理 | Pinia | 2.1+ | Vue状态管理 |
| 后端框架 | Spring Boot | 3.2+ | Java框架 |
| 后端语言 | Java | 17 LTS | JDK版本 |
| 数据存储 | JSON File | - | 单机模式存储 |
| 缓存 | Embedded Redis | 0.7.3 | 可选缓存 |
| 工具库 | Hutool | 5.8.26 | Java工具类 |

---

## 项目结构

```
cat-cat-cooperations/
├── cat-standalone/              # 单机启动模块（推荐）
│   ├── src/main/java/
│   │   ├── com/cat/standalone/  # 核心服务
│   │   └── com/cat/cliagent/    # CLI Agent服务
│   └── data/                    # JSON数据文件目录
│       ├── cli_agents.json      # Agent实例数据
│       ├── cli_agent_templates.json  # Agent模板数据
│       ├── tasks.json           # 任务数据
│       └── ...
├── cat-web/                     # Vue 3前端项目
│   ├── src/
│   │   ├── api/                 # API请求模块
│   │   ├── views/               # 页面组件
│   │   ├── stores/              # Pinia状态管理
│   │   └── utils/               # 工具函数
│   └── package.json
├── docs/                        # 文档目录
│   ├── architecture.md          # 架构说明书
│   ├── plans/                   # 设计文档
│   └── features/                # 功能文档
├── feature-list.json            # 功能进度跟踪
├── pom.xml                      # Maven配置
├── run-standalone.sh            # Linux/Mac启动脚本
└── run-standalone.bat           # Windows启动脚本
```

---

## 快速开始

### 环境要求

- **Java**: JDK 17+
- **Maven**: 3.8+
- **Node.js**: 18+ (前端开发需要)
- **操作系统**: Linux / macOS / Windows

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

启动成功后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:3000 | 需要单独启动前端 |
| 后端API | http://localhost:8080/api/v1 | RESTful API |
| WebSocket | ws://localhost:8080/ws | 实时通信 |

### 前端启动（可选）

```bash
cd cat-web
npm install
npm run dev
```

---

## 开发指南

### 后端开发

#### 项目构建

```bash
# 完整构建
mvn clean package -DskipTests

# 仅构建单机模块
mvn clean package -pl cat-standalone -am -DskipTests

# 运行测试
mvn test -pl cat-standalone
```

#### 数据存储

单机模式使用JSON文件存储数据，数据目录位于 `./cat-standalone/data/`：

| 文件 | 说明 |
|------|------|
| `cli_agents.json` | CLI Agent实例数据 |
| `cli_agent_templates.json` | Agent模板数据 |
| `cli_agent_capabilities.json` | Agent能力数据 |
| `tasks.json` | 任务数据 |
| `task_assignments.json` | 任务分配数据 |
| `task_logs.json` | 任务日志数据 |
| `token_usage_logs.json` | Token使用记录 |
| `cli_agent_output_logs.json` | CLI输出日志 |

### 前端开发

```bash
cd cat-web

# 安装依赖
npm install

# 开发服务器
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint
```

---

## 扩展开发

### 1. 添加新的CLI Agent模板

在 `cat-standalone/src/main/java/com/cat/standalone/store/entity/` 中定义模板实体，并在模板初始化服务中添加新模板：

```java
// 创建新模板
CliAgentTemplate template = new CliAgentTemplate();
template.setName("My Custom CLI");
template.setCliCommand("my-cli");
template.setRequiredParams(Arrays.asList("API_KEY"));
template.setOptionalParams(Arrays.asList("model", "timeout"));
// ... 保存模板
```

### 2. 添加新的任务类型

在 `cat-standalone/src/main/java/com/cat/cliagent/task/entity/Task.java` 中定义新类型：

```java
public enum TaskType {
    SIMPLE,
    WORKFLOW,
    PARALLEL,
    NEGOTIATION,
    CUSTOM // 新类型
}
```

然后在任务执行服务中实现对应的执行逻辑。

### 3. 添加新的API接口

遵循Controller → Service → Store的分层架构：

```java
// 1. 创建Controller
@RestController
@RequestMapping("/api/v1/custom")
public class CustomController {
    @Autowired
    private CustomService customService;

    @GetMapping
    public ApiResponse<List<CustomEntity>> list() {
        return ApiResponse.success(customService.list());
    }
}

// 2. 创建Service
@Service
public class CustomService {
    @Autowired
    private JsonFileStore<CustomEntity> store;

    public List<CustomEntity> list() {
        return store.findAll();
    }
}
```

### 4. 前端页面扩展

在 `cat-web/src/views/` 中添加新页面：

```vue
<!-- CustomView.vue -->
<template>
  <div class="custom-page">
    <h1>自定义页面</h1>
    <!-- 页面内容 -->
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
// 组件逻辑
</script>
```

然后在 `cat-web/src/router/index.ts` 中添加路由配置。

---

## API文档

### 认证相关

单机模式采用简化认证，任意用户名密码均可登录。

```bash
# 登录示例（实际无需调用，直接访问前端即可）
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "any"}'
```

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

### 任务管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/tasks` | GET | 获取任务列表 |
| `/api/v1/tasks` | POST | 创建任务 |
| `/api/v1/tasks/{id}` | GET | 获取任务详情 |
| `/api/v1/tasks/{id}/cancel` | POST | 取消任务 |

### WebSocket

连接地址：`ws://localhost:8080/ws`

用于实时接收Agent输出、Token使用更新等。

---

## 贡献指南

### 提交Issue

- 使用清晰的标题描述问题
- 提供详细的复现步骤
- 附上相关的错误日志

### 提交PR

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### 代码规范

- 后端：遵循Java编码规范，使用统一的代码格式化配置
- 前端：使用ESLint进行代码检查，遵循Vue 3风格指南

---

## 文档索引

- [架构说明书](docs/architecture.md) - 详细架构设计
- [功能列表](feature-list.json) - 功能进度跟踪
- [发布说明](RELEASE_NOTES.md) - 版本发布记录
- [CLAUDE.md](CLAUDE.md) - Claude Code开发指南

---

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

<p align="center">
  <img src="https://img.shields.io/badge/Made%20with-%F0%9F%90%B1-orange" alt="Made with cat">
</p>
