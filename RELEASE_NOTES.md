# 猫猫多Agent协同系统 - 发布说明

## 版本历史

### v1.1.0 (开发中 - Wave 1)

**发布日期**: 待定

**Wave 1 变更 - CLI Agent重构 (2026-04-04)**:

这是一次重大架构重构，将Agent管理从"内置执行器"模式转变为"CLI Agent工具对接"模式。

**新功能**:
- CLI Agent模板管理 - 内置Claude Code/OpenCode模板
- CLI Agent实例管理 - 基于模板创建Agent，配置启动参数和环境变量
- CLI进程生命周期管理 - 启动、监控、终止、异常重启
- CLI持久会话通信 - stdin/stdout持久会话，任务输入和流式输出
- 流式输出WebSocket推送 - CLI输出实时推送到前端
- CLI任务执行控制 - 超时控制、手动取消、并发限制
- Token使用统计 - 从CLI输出解析Token使用信息

**修改功能**:
- Agent能力管理 → 改为能力类型+领域标签模式
- Agent状态监控 → 新增进程状态、Token使用监控
- 任务执行 → 支持混合输入（Prompt/JSON/文件路径）
- 前端Agent管理页面 → 适配CLI Agent模式

**废弃功能**:
- 内置Agent执行器 (FEAT-004, FEAT-019~022)
- 外部Agent接入 (FEAT-007)
- MCP协议适配器 (FEAT-023)

**技术栈变更**:
- 新增CLI进程管理能力
- 新增stdin/stdout管道通信
- 敏感配置加密存储

**前端深色科技风UI重构 (2026-04-15)**:

- **全新设计系统**: 深色科技风（Dark Tech）主题，紫蓝渐变配色方案（Violet #7C3AED → Cyan #06B6D4）
- **SCSS设计令牌**: 全新 `_variables.scss` 定义深色调色板、语义色彩、边框、阴影、间距、圆角等设计令牌
- **Element Plus深色覆盖**: 通过CSS Custom Properties全局覆盖Element Plus组件为深色主题（对话框、选择器、标签、按钮、输入框等）
- **定制SVG图标系统**: 新增 `CatIcons.vue` 组件，包含12个科技风SVG图标（CatLogo、RobotAgent、GroupChat、Dashboard、Terminal、Lightning等）
- **AppLayout重构**: 深色侧边栏（渐变Logo文字、自定义导航、渐变激活指示条）+ 毛玻璃顶栏（backdrop-filter）+ 渐变环头像
- **仪表盘重构**: 深色统计卡片（渐变图标背景、渐变数值文字）、快速操作卡片
- **CLI Agent列表页**: 深色卡片样式、状态标签动画（运行中呼吸动效、执行中脉冲环动效）
- **群聊页面**: 深色聊天界面、紫色渐变消息气泡、暗色@提及弹窗、流式动画
- **Agent管理页**: 适配深色卡片主题
- **全局动画**: breathe、pulse-ring、gradient-shift CSS动画
- **工具类**: .gradient-text、.gradient-border CSS实用类
- **登录页**: 保持原有设计不变（独立配色方案）

**群聊功能修复 (2026-04-15)**:

- **群聊@提及输入优化**: 群聊输入框支持直接输入`@`触发Agent选择弹窗，支持按名称实时筛选、键盘上下箭头导航和Enter选择，同时保留原有的@按钮手动选择方式
- **群聊Agent上下文感知**: 群聊中Agent能够感知其他参与者的消息，发送给Agent的prompt包含最近20条群聊历史记录，标注各消息的发送者身份（用户/其他Agent/自己），使Agent理解完整的群聊对话上下文
- **群聊推送隔离**: 后端现在会根据群聊上下文路由 WebSocket 推送：群聊执行时不再把 `output / text_delta / error / done / EXECUTING` 推送到个人 topic，仅在任务结束后同步最终 `RUNNING` 状态
- **群聊错误收尾修复**: 群聊执行失败时会正确清理群聊上下文与 streaming spinner，并在群聊消息中追加系统错误提示，避免残留状态污染下一次会话

**移除功能**:

- **聊天室（ChatRoom）**: 移除聊天室页面及相关前端路由，Agent单聊功能可通过CLI Agent详情页进行

---

### v1.0.0 (开发中)

**发布日期**: 待定

**新功能**:
- 用户认证与授权
- Agent管理（内置/外部）
- 任务创建与执行
- 多Agent协同机制
- 系统监控与告警
- 猫猫主题前端界面

**技术栈**:
- 后端: Spring Boot 3.2 + Java 17
- 前端: Vue 3 + TypeScript + Element Plus
- 数据库: MySQL 8.0 + Redis 7.0

---

## 开发进度

### Milestone 6: CLI Agent重构 (Wave 1)
**状态**: ✅ 已完成
**完成日期**: 2026-04-05

功能列表:
- [x] FEAT-031: CLI Agent模板管理 ✅
- [x] FEAT-032: CLI Agent实例管理 ✅
- [x] FEAT-033: CLI进程生命周期管理 ✅
- [x] FEAT-034: CLI持久会话通信 ✅
- [x] FEAT-035: 流式输出WebSocket推送 ✅
- [x] FEAT-036: CLI任务执行控制 ✅
- [x] FEAT-037: Token使用统计 ✅
- [x] FEAT-005: CLI Agent能力管理 (修改) ✅
- [x] FEAT-006: CLI Agent状态监控 (修改) ✅
- [x] FEAT-008: CLI Agent任务执行 (修改) ✅
- [x] FEAT-014: Agent间消息通信 (修改) ✅
- [x] FEAT-027: 前端CLI Agent管理页面 (修改) ✅

### Milestone 1: 基础认证和Agent管理
**状态**: ✅ 已完成
**目标日期**: 2026-03-15

功能列表:
- [x] FEAT-001: 用户认证模块
- [x] FEAT-004: 内置Agent创建与管理
- [x] FEAT-005: Agent能力管理
- [x] FEAT-006: Agent状态监控

### Milestone 2: 核心任务执行
**状态**: ✅ 已完成
**目标日期**: 2026-03-22

功能列表:
- [x] FEAT-008: 简单任务创建与执行
- [x] FEAT-013: 任务状态追踪
- [x] FEAT-019: 内置Agent命令执行器
- [x] FEAT-020: 内置Agent API调用执行器

### Milestone 3: 前端核心页面
**状态**: ✅ 已完成
**目标日期**: 2026-03-29

功能列表:
- [x] FEAT-025: 前端登录页面
- [x] FEAT-026: 前端仪表盘页面
- [x] FEAT-027: 前端Agent管理页面
- [x] FEAT-028: 前端任务管理页面

### Milestone 4: 高级功能完善
**状态**: ✅ 已完成
**目标日期**: 2026-04-05

功能列表:
- [x] FEAT-010: 流程化任务编排
- [x] FEAT-011: 并行任务处理
- [x] FEAT-014: Agent间消息通信
- [x] FEAT-015: WebSocket连接管理
- [x] FEAT-023: MCP协议适配器

### Milestone 5: 监控和运维
**状态**: ✅ 已完成
**目标日期**: 2026-04-12

功能列表:
- [x] FEAT-016: 操作审计日志
- [x] FEAT-017: 系统监控服务
- [x] FEAT-018: 告警服务
- [x] FEAT-029: 前端监控页面

---

## 架构变更记录

### 2026-04-15
- **前端深色科技风UI重构**:
  - 全新SCSS设计系统：`_variables.scss` 定义深色调色板令牌（$bg-deep, $color-violet, $color-cyan等）
  - `main.scss` 使用 `@use` 现代Sass语法，全局Element Plus CSS Custom Properties深色覆盖
  - 新增 `CatIcons.vue`：12个SVG图标组件（CatLogo, RobotAgent, GroupChatIcon等）
  - `AppLayout.vue` 完全重写：深色侧边栏 + 毛玻璃顶栏 + 自定义导航
  - `DashboardView.vue` 完全重写：深色统计卡片 + 渐变数值 + SVG图标
  - `CliAgentListView.vue` 样式重写：深色卡片 + 状态动画（breathe/pulse-ring）
  - `GroupChatView.vue` 样式重写：深色聊天 + 渐变消息气泡
  - `AgentListView.vue` 样式重写：适配深色主题
  - 登录页保持独立配色方案不变

### 2026-04-07
- **Token使用记录持久化**: 将内存存储改为JSON文件持久化
  - 新增 `StoredTokenUsageLog` 实体类
  - 新增 `token_usage_logs.json` 数据文件
  - `LocalTokenUsageService` 使用 `JsonFileStore` 替代内存 Map
  - 支持按Agent统计、时间范围查询、过期记录清理
- **CLI Agent输出日志持久化**: 将内存存储改为JSON文件持久化
  - 新增 `StoredCliAgentOutputLog` 实体类
  - 新增 `cli_agent_output_logs.json` 数据文件
  - `LocalCliSessionService` 使用 `JsonFileStore` 替代内存 Map
  - 每Agent最多保留100条日志，自动清理旧记录
- **StoreConfig更新**: 新增两个存储Bean配置
  - `tokenUsageLogStore` - Token使用记录存储
  - `cliAgentOutputLogStore` - CLI输出日志存储

### 2026-04-06 - 项目简化：移除微服务模块

**变更描述**: 移除所有微服务专用模块，项目简化为独立模式(standalone)专用架构。

**已删除模块**:
- cat-common - 原共享工具类模块（standalone已独立）
- cat-auth - 原JWT认证服务（standalone使用简化认证）
- cat-agent - 原Agent管理服务（standalone内置）
- cat-task - 原任务管理服务（standalone内置）
- cat-orchestration - 原工作流引擎（不再需要）
- cat-message - 原WebSocket消息服务（standalone内置）
- cat-monitor - 原监控服务（不再需要）
- cat-audit - 原审计服务（不再需要）
- cat-runtime - 原内置执行器（已被CLI Agent替代）
- cat-gateway - 原API网关（微服务专用）
- cat-api - 空目录

**已删除文件**:
- docker-compose.yml - 微服务基础设施编排
- start.sh / start.bat - 微服务模式启动脚本
- scripts/init-database.sql - MySQL初始化脚本

**保留模块**:
- cat-standalone - 独立运行核心模块（229KB JAR）
- cat-web - Vue 3前端应用

**项目结构变更**:
```
# 变更前（微服务+独立混合）
cat-cat-cooperations/
├── cat-common/        # 共享模块
├── cat-auth/          # 认证服务
├── cat-agent/         # Agent管理
├── cat-task/          # 任务管理
├── ...
├── cat-standalone/    # 独立模式
└── cat-web/           # 前端

# 变更后（纯独立模式）
cat-cat-cooperations/
├── cat-standalone/    # 后端核心（JSON存储+嵌入式Redis）
└── cat-web/           # 前端（Vue 3）
```

**pom.xml变更**:
- modules列表: 11个 → 1个（仅cat-standalone）
- 移除Spring Cloud依赖管理
- 移除MyBatis Plus依赖管理
- 移除JWT依赖管理（standalone使用简化认证）

**数据存储**:
- 原MySQL表结构 → JSON文件（./data/目录）
- 原外部Redis → 嵌入式Redis（可选，端口6380）

**启动方式**:
```bash
# 简化为单一命令
mvn spring-boot:run -pl cat-standalone
```

**技术债务清理**:
- FEAT-001~003（用户认证/管理/权限）- 标记为deprecated，standalone使用简化认证
- FEAT-024（API网关路由）- 标记为deprecated，standalone不需要网关
- FEAT-016~018（审计/监控/告警）- 标记为deprecated，非核心功能
- 所有标注module为cat-auth/cat-agent/cat-task等的features更新为cat-standalone

### 2026-04-06
- **群聊功能增强**:
  - 群聊@提及输入优化
  - 群聊Agent上下文感知
  - 群聊推送隔离修复
- **聊天室功能移除**:
  - 移除聊天室（ChatRoom）页面和前端路由
  - 移除侧边栏聊天室菜单入口
- **日志中文乱码修复**:
  - 新增 `logback-spring.xml` 配置文件，明确指定 UTF-8 编码
  - 更新 `application.yml` 添加日志编码配置
  - 新增 `run.bat` 启动脚本，设置控制台编码为 UTF-8
- **WebSocket 连接修复**:
  - 新增 Vite WebSocket 代理配置 (`/ws` 端点)
  - 前端 WebSocket 使用相对路径连接
- **CLI Agent 启动修复**:
  - 移除 `CLAUDECODE` 环境变量，允许在 Claude Code 会话中嵌套运行
  - 更新模板默认参数配置
- **数据模型更新**:
  - `StoredCliAgent` 新增 `sessionId` 字段，用于恢复对话上下文（预留）
- **已知问题**:
  - 多轮对话功能未完全实现：CLI 进程 stdout 读取线程在首次响应后退出，后续消息无法获取响应

### 2026-04-04
- **仪表盘数据真实化**: 新增后端统计API接口
  - 新增 `DashboardController` 提供实时统计数据
  - 前端仪表盘现在从API获取真实的Agent数量、任务数量、成功率、平均耗时
  - 统计API: `GET /api/v1/dashboard/stats`
- **添加删除功能**:
  - Agent管理页面新增删除按钮和删除确认功能
  - 任务管理页面新增删除按钮和删除确认功能
- **修复任务API**:
  - 取消任务: `PUT /tasks/${id}/cancel` → `POST /tasks/${id}/cancel`
  - 重试任务: `PUT /tasks/${id}/retry` → `POST /tasks/${id}/retry`
- **新增重试任务API**: 后端新增 `POST /api/v1/tasks/{taskId}/retry` 接口

### 2026-04-03 (续2)
- **修复前后端API不匹配问题**: 统一前端API参数和返回类型与后端保持一致
  - 分页参数：`size` → `pageSize`，`keyword` → `name/username`
  - 分页返回：`records` → `items`，`current` → `page`，`size` → `pageSize`，`pages` → `totalPages`
  - Agent启用/禁用：`PUT /agents/${id}/enable` → `POST /agents/${id}/actions/enable`
  - 涉及文件：agent.ts、task.ts、user.ts、monitor.ts、AgentListView.vue、TaskListView.vue

### 2026-04-03 (续)
- **简化认证流程**: 为方便本地开发和使用，移除了前后端的认证验证
  - 前端：去掉路由守卫，登录页面直接跳转，无需后端验证
  - 后端：SimpleAuthInterceptor不再验证Token，允许所有请求通过
  - 登录页面：任意用户名密码可直接进入系统
  - 适用场景：本地开发、演示、测试环境

### 2026-04-03
- **新增cat-standalone模块**: 轻量级单机启动版本，无需外部依赖
  - **JSON文件存储替代数据库**: 使用本地JSON文件持久化，无需MySQL/H2
  - 内嵌Redis服务器替代外部Redis (可选用于缓存)
  - 简化认证系统（内存Token存储）
  - 默认用户: admin/admin123
- **移除所有数据库依赖**: 单机模式使用纯JSON文件存储
  - agents.json - Agent实体数据
  - tasks.json - Task实体数据
  - capabilities.json - Agent能力数据
  - assignments.json - 任务分配数据
  - task_logs.json - 任务日志数据
- **移除Redis依赖**: 单机模式使用embedded-redis (可选)
- **JwtUtils迁移**: 从cat-common移动到cat-auth模块
- **代码修复**: TextExecutor Java 17兼容性、CommandExecutor exitValue方法
- **启动脚本**: 添加run-standalone.bat/sh快速启动脚本
- **API认证**: 使用X-Cat-Token header进行认证

### 2026-04-02
- **移除RabbitMQ依赖**: 经分析代码中无实际使用，已从docker-compose.yml中移除
- **新增cat-message模块**: 实现WebSocket连接管理和Agent间消息通信 (Port: 8500)
- **完善WorkflowEngine**: 添加实际步骤执行逻辑、重试机制、Redis持久化
- **实现告警服务**: 完整的规则管理、定时检查、告警生命周期管理
- **实现MCP协议适配器**: 工具注册、调用、资源访问、调用日志
- **前端API集成**: 创建agent.ts、task.ts、monitor.ts、user.ts API模块

---

## 已知问题

无已知问题。

---

## 历史问题（已修复）

### 2026-04-06 - CLI Agent 多轮对话问题 ✅ 已修复

- **问题**：发送第一条消息可以收到响应，但后续消息无法获取响应
- **原因**：CLI 进程 stdout 读取线程在 `readLine()` 返回 null 后退出，后续发送消息时无法读取输出
- **解决方案**：改用 `--print --output-format stream-json` 模式，每次请求独立执行，使用 `--resume` 恢复会话上下文
- **实现**：
  - 重写 `LocalCliSessionService`：每请求启动独立进程，通过 `stream-json` 格式解析输出
  - 重写 `LocalCliProcessService`：简化进程管理，不再维护持久进程
  - 新增 WebSocket 消息类型 `text_delta` 和 `done` 支持流式输出
  - 前端 `ChatRoomView.vue` 适配新的消息类型
- **影响文件**：
  - `LocalCliSessionService.java` - 完全重写，支持 --print 模式
  - `LocalCliProcessService.java` - 简化，移除持久进程管理
  - `ChatRoomView.vue` - 新增 text_delta 和 done 消息处理
  - `CliOutputPushService.java` / `LocalCliOutputPushService.java` - 新增 pushTextDelta, pushDone 方法

---

## 升级指南

### 单机模式快速启动 (推荐新用户)

```bash
# 构建并启动
mvn clean package -pl cat-standalone -am -DskipTests
java -jar cat-standalone/target/cat-standalone-1.0.0-SNAPSHOT.jar

# 或使用Maven直接运行
mvn spring-boot:run -pl cat-standalone

# 或使用启动脚本
./run-standalone.sh   # Linux/Mac
run-standalone.bat    # Windows
```

**访问地址:**
- API: http://localhost:8080/api/v1
- 默认用户: admin / admin123
- 数据目录: ./data/

**认证方式:**
```bash
# 1. 登录获取Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
# 返回: {"code":0,"message":"success","data":{"token":"cat-xxx...",...}}

# 2. 使用X-Cat-Token header访问API
curl http://localhost:8080/api/v1/agents \
  -H "X-Cat-Token: cat-xxx..."
```

### 从旧版本升级 (微服务模式)

如果之前启动过包含RabbitMQ的容器，执行以下步骤：

```bash
# 停止并删除旧容器
docker-compose down

# 删除RabbitMQ数据卷（可选）
docker volume rm cat-cat-cooperations_rabbitmq_data

# 启动新版本
docker-compose up -d
```

### 数据迁移

单机模式数据存储在JSON文件中，位于`./data/`目录：
- agents.json - Agent实体数据
- tasks.json - Task实体数据
- capabilities.json - Agent能力数据
- assignments.json - 任务分配数据
- task_logs.json - 任务日志数据
- cli_agents.json - CLI Agent实例数据
- cli_agent_templates.json - CLI Agent模板数据
- cli_agent_capabilities.json - CLI Agent能力数据
- token_usage_logs.json - Token使用记录
- cli_agent_output_logs.json - CLI Agent输出日志

如需迁移数据，可以直接编辑JSON文件或使用API导入。
