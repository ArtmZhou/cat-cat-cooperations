# 猫猫多Agent协同系统 - 架构设计文档

**文档编号:** CAT-AGENT-DESIGN-001
**版本:** 1.1
**日期:** 2026-04-04
**状态:** 待审批
**关联SRS:** CAT-AGENT-SRS-001
**关联UCD:** CAT-AGENT-UCD-001

> **Wave 1 变更说明 (2026-04-04):**
> 本次更新将Agent管理从"内置执行器"模式重构为"CLI Agent工具对接"模式。
> 第一阶段支持Claude Code和OpenCode两种CLI工具。
> 影响范围：第2章架构图、第3.4章cat-agent模块、第3.10章cat-runtime模块、第4章API设计。

---

## 1. 概述

### 1.1 文档目的

本文档定义猫猫多Agent协同系统的技术架构和详细设计，为开发团队提供实施蓝图。

### 1.2 设计原则

| 原则 | 描述 |
|------|------|
| 模块化 | 各模块高内聚低耦合，便于独立开发和测试 |
| 可扩展 | 架构支持水平扩展，适应业务增长 |
| 安全性 | 安全设计贯穿各层，防御性编程 |
| 可观测 | 完善的日志、监控、追踪能力 |
| 简洁性 | 在满足需求前提下选择最简方案 |

### 1.3 技术选型

| 层级 | 技术选型 | 版本 | 选型理由 |
|------|----------|------|----------|
| **前端框架** | Vue 3 | 3.4+ | 组合式API、TypeScript原生支持、生态成熟 |
| **前端UI库** | Element Plus | 2.5+ | Vue3生态成熟组件库、易定制主题 |
| **前端构建** | Vite | 5.0+ | 极速开发体验、HMR高效 |
| **前端语言** | TypeScript | 5.0+ | 类型安全、IDE支持好 |
| **后端框架** | Spring Boot | 3.2+ | Java生态成熟、企业级特性完善 |
| **后端语言** | Java | 17 LTS | 长期支持版本、性能优秀 |
| **数据库** | MySQL | 8.0+ | 事务支持完善、运维成熟 |
| **缓存** | Redis | 7.0+ | 高性能缓存、支持多种数据结构 |
| **容器化** | Docker | 24.0+ | 标准化部署、环境一致 |

> **注：** RabbitMQ在实现过程中经分析代码无实际使用，已于2026-04-02从项目中移除。

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           客户端层 (Client Layer)                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐        │
│  │   Web Browser   │  │  External Agent │  │   Admin CLI     │        │
│  │   (Vue 3 SPA)   │  │   (Multi-Proto) │  │   (Optional)    │        │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘        │
└───────────┼─────────────────────┼─────────────────────┼─────────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          接入层 (Gateway Layer)                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      API Gateway (Spring Cloud Gateway)          │   │
│  │   • 路由转发  • 认证鉴权  • 限流熔断  • 日志记录  • 协议转换     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌───────────────────┐  ┌───────────────────┐  ┌────────────────────┐  │
│  │    REST API       │  │   WebSocket       │  │      gRPC          │  │
│  │    /api/v1/*      │  │   /ws/*           │  │      :9090         │  │
│  └───────────────────┘  └───────────────────┘  └────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         服务层 (Service Layer)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Auth      │  │   Agent     │  │    Task     │  │  Orchestration│  │
│  │   Service   │  │   Service   │  │   Service   │  │    Engine     │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬───────┘   │
│         │                │                │                │           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   User      │  │  Message    │  │  Monitor    │  │   Audit      │  │
│  │   Service   │  │   Service   │  │   Service   │  │   Service    │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬───────┘   │
└─────────┼────────────────┼────────────────┼────────────────┼───────────┘
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Agent运行时 (Agent Runtime)                       │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    CLI Agent Runtime (Wave 1)                     │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │  CLI Process Manager    │  Session Manager   │  Output Parser│  │  │
│  │  │  • Claude Code          │  • stdin/stdout   │  • 流式响应   │  │  │
│  │  │  • OpenCode             │  • 持久会话       │  • Token统计  │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │  Agent Template Store   │  Capability Matcher│  Status Monitor│  │  │
│  │  │  • 模板配置             │  • 能力匹配        │  • 进程监控   │  │  │
│  │  │  • 参数模板             │  • 领域标签        │  • 资源占用   │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  [DEPRECATED - Wave 1] Built-in Agent Runtime (已废弃)                   │
│  [DEPRECATED - Wave 1] External Agent Adapter (已废弃)                   │
└─────────────────────────────────────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         数据层 (Data Layer)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   MySQL     │  │   Redis     │  │  RabbitMQ   │  │ File Storage│   │
│  │  主数据存储  │  │ 缓存/会话   │  │  消息队列   │  │  文件存储    │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 架构分层说明

| 层级 | 职责 | 组件 |
|------|------|------|
| 客户端层 | 用户交互、CLI Agent接入 | Web应用、CLI Agent进程 |
| 接入层 | 请求路由、认证、限流 | API Gateway |
| 服务层 | 业务逻辑处理 | 微服务集群 |
| Agent运行时 | CLI Agent进程管理 | CLI进程管理器、会话管理器、输出解析器 |
| 数据层 | 数据持久化 | MySQL、Redis、File Storage |

---

## 3. 模块详细设计

### 3.1 模块总览

```
cat-agent-platform/
├── cat-gateway/                    # API网关模块
├── cat-auth/                       # 认证授权模块
├── cat-agent/                      # Agent管理模块
├── cat-task/                       # 任务管理模块
├── cat-orchestration/              # 协同引擎模块
├── cat-message/                    # 消息服务模块
├── cat-monitor/                    # 监控服务模块
├── cat-audit/                      # 审计服务模块
├── cat-runtime/                    # Agent运行时模块
├── cat-common/                     # 公共模块
├── cat-api/                        # API定义模块
└── cat-web/                        # 前端应用
```

### 3.2 cat-gateway (API网关模块)

#### 3.2.1 职责
- 统一入口，路由分发
- 认证token验证
- 请求限流熔断
- 请求日志记录
- 协议转换（REST/WebSocket/gRPC）

#### 3.2.2 关键配置

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://cat-auth
          predicates:
            - Path=/api/v1/auth/**
        - id: agent-service
          uri: lb://cat-agent
          predicates:
            - Path=/api/v1/agents/**
        - id: task-service
          uri: lb://cat-task
          predicates:
            - Path=/api/v1/tasks/**
        - id: websocket-route
          uri: lb:ws://cat-message
          predicates:
            - Path=/ws/**
```

### 3.3 cat-auth (认证授权模块)

#### 3.3.1 核心类设计

```java
// 用户认证服务
public interface AuthService {
    LoginResult login(LoginRequest request);
    void logout(String token);
    UserInfo validateToken(String token);
    RefreshResult refreshToken(String refreshToken);
}

// 用户服务
public interface UserService {
    User createUser(CreateUserRequest request);
    User updateUser(String userId, UpdateUserRequest request);
    void deleteUser(String userId);
    User getUser(String userId);
    Page<User> listUsers(UserQuery query);
}

// 角色权限服务
public interface RoleService {
    Role createRole(CreateRoleRequest request);
    void assignRole(String userId, String roleId);
    boolean hasPermission(String userId, String permission);
    List<Permission> getUserPermissions(String userId);
}
```

#### 3.3.2 数据模型

```sql
CREATE TABLE `user` (
    `id` VARCHAR(36) PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100),
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(255),
    `status` TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_status` (`status`)
);

CREATE TABLE `role` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255),
    `is_system` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `permission` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(100) NOT NULL UNIQUE,
    `resource_type` VARCHAR(50) COMMENT 'menu/button/api',
    `resource_path` VARCHAR(255),
    `parent_id` VARCHAR(36),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user_role` (
    `id` VARCHAR(36) PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `role_id` VARCHAR(36) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`role_id`) REFERENCES `role`(`id`)
);

CREATE TABLE `role_permission` (
    `id` VARCHAR(36) PRIMARY KEY,
    `role_id` VARCHAR(36) NOT NULL,
    `permission_id` VARCHAR(36) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    FOREIGN KEY (`role_id`) REFERENCES `role`(`id`),
    FOREIGN KEY (`permission_id`) REFERENCES `permission`(`id`)
);
```

### 3.4 cat-agent (Agent管理模块)

> **Wave 1 更新 (2026-04-04):** 本节已重构为CLI Agent管理模式。
> 原有"内置Agent"和"外部Agent"概念已废弃，统一为"CLI Agent"。

#### 3.4.1 核心类设计 (Wave 1)

```java
// CLI Agent模板服务
public interface CliAgentTemplateService {
    // 获取内置模板列表（Claude Code, OpenCode等）
    List<CliAgentTemplate> getBuiltInTemplates();
    CliAgentTemplate getTemplate(String templateId);
    // 创建自定义模板
    CliAgentTemplate createTemplate(CliAgentTemplateCreateRequest request);
}

// CLI Agent实例服务
public interface CliAgentService {
    // 基于模板创建Agent实例
    CliAgent createAgent(CliAgentCreateRequest request);
    CliAgent getAgent(String agentId);
    void updateAgent(String agentId, CliAgentUpdateRequest request);
    void deleteAgent(String agentId);
    Page<CliAgent> listAgents(CliAgentQuery query);

    // Agent启停（启动/停止CLI进程）
    void startAgent(String agentId);
    void stopAgent(String agentId);
    void restartAgent(String agentId);

    // 获取Agent状态
    CliAgentStatus getAgentStatus(String agentId);
    CliAgentMetrics getAgentMetrics(String agentId);
}

// CLI Agent能力服务
public interface CliAgentCapabilityService {
    // 声明能力（能力类型 + 领域标签）
    void declareCapabilities(String agentId, List<CliCapability> capabilities);
    List<CliCapability> getAgentCapabilities(String agentId);
    // 根据能力匹配Agent
    List<CliAgent> findAgentsByCapability(String capabilityType, List<String> domainTags);
    // 自动匹配最适合的Agent
    CliAgent matchBestAgent(TaskRequirement requirement);
}

// CLI进程管理服务
public interface CliProcessService {
    // 进程生命周期
    ProcessHandle startProcess(String agentId);
    void stopProcess(String agentId);
    void restartProcess(String agentId);

    // 进程状态
    ProcessStatus getProcessStatus(String agentId);
    boolean isProcessHealthy(String agentId);

    // 进程监控
    ProcessMetrics getProcessMetrics(String agentId);
    List<ProcessLog> getProcessLogs(String agentId, int limit);
}

// CLI会话管理服务
public interface CliSessionService {
    // 建立持久会话
    CliSession establishSession(String agentId);
    void closeSession(String agentId);

    // 任务输入输出
    void sendInput(String agentId, String input);
    void sendTask(String agentId, TaskInput taskInput);
    CliOutput subscribeOutput(String agentId, Consumer<StreamOutput> callback);

    // 流式输出
    Flux<StreamOutput> streamOutput(String agentId);
}

// Token使用统计服务
public interface TokenUsageService {
    // 从CLI输出解析Token使用
    TokenUsage parseFromOutput(String agentId, String output);
    // 记录Token使用
    void recordUsage(String agentId, TokenUsage usage);
    // 查询Token使用统计
    TokenUsageStats getUsageStats(String agentId, TimeRange range);
}
```

#### 3.4.2 数据模型 (Wave 1)

```sql
-- CLI Agent模板表
CREATE TABLE `cli_agent_template` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称，如Claude Code, OpenCode',
    `cli_type` VARCHAR(50) NOT NULL COMMENT 'CLI类型标识',
    `description` VARCHAR(500),
    `executable_path` VARCHAR(500) COMMENT '默认CLI路径',
    `default_args` JSON COMMENT '默认启动参数',
    `required_env_vars` JSON COMMENT '必需的环境变量列表',
    `optional_env_vars` JSON COMMENT '可选的环境变量列表',
    `config_template` JSON COMMENT '配置文件模板',
    `output_format` VARCHAR(20) COMMENT '输出格式: STREAM/JSON',
    `token_parsing_rule` JSON COMMENT 'Token解析规则',
    `is_builtin` BOOLEAN DEFAULT FALSE COMMENT '是否内置模板',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_cli_type` (`cli_type`)
);

-- CLI Agent实例表
CREATE TABLE `cli_agent` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `template_id` VARCHAR(36) NOT NULL COMMENT '关联模板ID',
    `status` VARCHAR(20) DEFAULT 'STOPPED' COMMENT 'STOPPED/STARTING/RUNNING/EXECUTING/ERROR',
    `executable_path` VARCHAR(500) COMMENT 'CLI可执行文件路径',
    `args` JSON COMMENT '启动参数',
    `env_vars` JSON COMMENT '环境变量（含敏感信息加密存储）',
    `config_path` VARCHAR(500) COMMENT '配置文件路径',
    `working_dir` VARCHAR(500) COMMENT '工作目录',
    `process_id` VARCHAR(50) COMMENT '系统进程ID',
    `last_started_at` DATETIME,
    `last_stopped_at` DATETIME,
    `created_by` VARCHAR(36),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_by` (`created_by`),
    FOREIGN KEY (`template_id`) REFERENCES `cli_agent_template`(`id`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

-- CLI Agent能力表
CREATE TABLE `cli_agent_capability` (
    `id` VARCHAR(36) PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL,
    `capability_type` VARCHAR(50) NOT NULL COMMENT 'CODE_GEN/FILE_OP/API_CALL/DATA_PROC等',
    `capability_name` VARCHAR(100) NOT NULL,
    `domain_tags` JSON COMMENT '领域标签，如["frontend", "backend", "python"]',
    `description` VARCHAR(255),
    `proficiency_level` INT DEFAULT 1 COMMENT '熟练度1-5',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_agent_id` (`agent_id`),
    INDEX `idx_type` (`capability_type`),
    FOREIGN KEY (`agent_id`) REFERENCES `cli_agent`(`id`) ON DELETE CASCADE
);

-- CLI Agent指标表
CREATE TABLE `cli_agent_metric` (
    `id` VARCHAR(36) PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL,
    `tasks_total` INT DEFAULT 0,
    `tasks_success` INT DEFAULT 0,
    `tasks_failed` INT DEFAULT 0,
    `avg_duration_ms` BIGINT DEFAULT 0,
    `total_tokens_used` BIGINT DEFAULT 0,
    `last_task_at` DATETIME,
    `cpu_usage_percent` DECIMAL(5,2),
    `memory_usage_mb` DECIMAL(10,2),
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_agent_id` (`agent_id`),
    FOREIGN KEY (`agent_id`) REFERENCES `cli_agent`(`id`) ON DELETE CASCADE
);

-- Token使用记录表
CREATE TABLE `token_usage_log` (
    `id` VARCHAR(36) PRIMARY KEY,
    `agent_id` VARCHAR(36) NOT NULL,
    `task_id` VARCHAR(36),
    `input_tokens` BIGINT DEFAULT 0,
    `output_tokens` BIGINT DEFAULT 0,
    `total_tokens` BIGINT DEFAULT 0,
    `model_name` VARCHAR(100),
    `cost_usd` DECIMAL(10,6),
    `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_agent_id` (`agent_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_recorded_at` (`recorded_at`),
    FOREIGN KEY (`agent_id`) REFERENCES `cli_agent`(`id`) ON DELETE CASCADE
);
```

#### 3.4.3 CLI Agent状态机 (Wave 1)

```
                    ┌──────────────────────────────────────────┐
                    │                                          │
                    ▼                                          │
              ┌──────────┐    start()     ┌───────────┐        │
              │ STOPPED  │ ──────────────▶│ STARTING  │        │
              └────┬─────┘                └─────┬─────┘        │
                   │                            │              │
                   │                      success│              │
                   │                            ▼              │
                   │                      ┌───────────┐        │
                   │                      │  RUNNING  │────────┘
                   │                      └─────┬─────┘  assign_task
                   │                            │              │
                   │                      execute│              │
                   │                            ▼              │
                   │                      ┌───────────┐        │
                   │                      │ EXECUTING │────────┘
                   │                      └─────┬─────┘  complete
                   │                            │
                   │    stop()            error │
                   │                            ▼
                   │                      ┌───────────┐
                   └─────────────────────▶│   ERROR   │
                                          └─────┬─────┘
                                                │ restart()
                                                │
                                                ▼
                                          ┌───────────┐
                                          │  RECOVER  │───▶ RUNNING
                                          └───────────┘
```

#### 3.4.4 CLI Agent模板定义 (Wave 1)

**Claude Code 模板示例:**
```yaml
templateId: claude-code
name: Claude Code
cliType: claude
description: Anthropic Claude CLI工具
executablePath: claude  # 或完整路径
defaultArgs:
  - "--output-format"
  - "stream-json"
requiredEnvVars:
  - ANTHROPIC_API_KEY
optionalEnvVars:
  - CLAUDE_MODEL
  - CLAUDE_MAX_TOKENS
configTemplate: null
outputFormat: STREAM_JSON
tokenParsingRule:
  type: json_field
  field: "$.usage"
  inputTokensField: "input_tokens"
  outputTokensField: "output_tokens"
```

**OpenCode 模板示例:**
```yaml
templateId: opencode
name: OpenCode
cliType: opencode
description: 开源代码助手CLI
executablePath: opencode
defaultArgs:
  - "--stream"
requiredEnvVars:
  - OPENAI_API_KEY
optionalEnvVars:
  - OPENAI_MODEL
outputFormat: STREAM
tokenParsingRule:
  type: regex
  pattern: "tokens:\\s*(\\d+)/(\\d+)"
```

#### 3.4.5 [DEPRECATED - Wave 1] 原Agent设计

> **废弃说明:** 以下设计已废弃，保留用于历史参考。

<details>
<summary>点击查看原Agent设计（已废弃）</summary>

```java
// [DEPRECATED] Agent管理服务
public interface AgentService {
    Agent registerAgent(AgentRegistration registration);
    Agent getAgent(String agentId);
    void updateAgent(String agentId, AgentUpdate update);
    void deleteAgent(String agentId);
    void enableAgent(String agentId);
    void disableAgent(String agentId);
    Page<Agent> listAgents(AgentQuery query);
    List<Agent> getAvailableAgents();
    AgentStatus getAgentStatus(String agentId);
}

// [DEPRECATED] Agent能力服务
public interface AgentCapabilityService {
    void registerCapabilities(String agentId, List<Capability> capabilities);
    List<Capability> getAgentCapabilities(String agentId);
    List<Agent> findAgentsByCapability(String capabilityType);
}

// [DEPRECATED] Agent心跳服务
public interface AgentHeartbeatService {
    void heartbeat(String agentId, HeartbeatInfo info);
    void checkTimeout();
}

// [DEPRECATED] 外部Agent接入服务
public interface ExternalAgentService {
    String generateAccessCredential(String agentId);
    AgentCredential validateCredential(String credential);
    void connect(String agentId, AgentConnection connection);
    void disconnect(String agentId);
}
```

原数据模型（已废弃）:
```sql
-- [DEPRECATED] 原agent表
CREATE TABLE `agent` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(500),
    `type` VARCHAR(20) NOT NULL COMMENT 'BUILT_IN/EXTERNAL',
    `status` VARCHAR(20) DEFAULT 'OFFLINE' COMMENT 'OFFLINE/ONLINE/BUSY/ERROR/DISABLED',
    `access_key` VARCHAR(64) COMMENT '外部Agent接入密钥',
    `config` JSON COMMENT 'Agent配置JSON',
    `metadata` JSON COMMENT '元数据',
    `last_heartbeat` DATETIME,
    ...
);
```

</details>

### 3.5 cat-task (任务管理模块)

#### 3.5.1 核心类设计

```java
// 任务服务
public interface TaskService {
    Task createTask(CreateTaskRequest request);
    Task getTask(String taskId);
    void updateTask(String taskId, TaskUpdate update);
    void cancelTask(String taskId);
    void deleteTask(String taskId);
    Page<Task> listTasks(TaskQuery query);
    TaskResult getTaskResult(String taskId);
    List<TaskLog> getTaskLogs(String taskId);
}

// 任务调度服务
public interface TaskSchedulerService {
    void scheduleTask(String taskId);
    void scheduleDelayedTask(String taskId, LocalDateTime executeAt);
    void schedulePeriodicTask(String taskId, CronExpression cron);
    void cancelSchedule(String taskId);
}

// 任务分配服务
public interface TaskAssignmentService {
    AssignmentResult assignTask(String taskId);
    List<Agent> selectAgents(Task task, AssignmentStrategy strategy);
    void reassignTask(String taskId, String reason);
}

// 任务执行服务
public interface TaskExecutionService {
    void startExecution(String taskId);
    void reportProgress(String taskId, ProgressInfo progress);
    void completeTask(String taskId, TaskResult result);
    void failTask(String taskId, ErrorInfo error);
}
```

#### 3.5.2 数据模型

```sql
CREATE TABLE `task` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000),
    `type` VARCHAR(30) NOT NULL COMMENT 'SIMPLE/WORKFLOW/PARALLEL/NEGOTIATION',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/ASSIGNED/RUNNING/COMPLETED/FAILED/CANCELLED',
    `priority` INT DEFAULT 0 COMMENT '0-低 1-中 2-高',
    `input` JSON COMMENT '任务输入参数',
    `output` JSON COMMENT '任务输出结果',
    `config` JSON COMMENT '任务配置',
    `timeout_seconds` INT DEFAULT 3600,
    `retry_count` INT DEFAULT 0,
    `max_retry` INT DEFAULT 3,
    `scheduled_at` DATETIME COMMENT '计划执行时间',
    `started_at` DATETIME COMMENT '实际开始时间',
    `completed_at` DATETIME COMMENT '完成时间',
    `created_by` VARCHAR(36) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`),
    INDEX `idx_type` (`type`),
    INDEX `idx_priority` (`priority`),
    INDEX `idx_created_by` (`created_by`),
    INDEX `idx_scheduled_at` (`scheduled_at`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`)
);

CREATE TABLE `task_assignment` (
    `id` VARCHAR(36) PRIMARY KEY,
    `task_id` VARCHAR(36) NOT NULL,
    `agent_id` VARCHAR(36) NOT NULL,
    `role` VARCHAR(50) COMMENT 'MAIN/HELPER/VOTER',
    `status` VARCHAR(20) DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED/RUNNING/COMPLETED/FAILED',
    `assigned_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `started_at` DATETIME,
    `completed_at` DATETIME,
    `result` JSON,
    `error_message` TEXT,
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_agent_id` (`agent_id`),
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`agent_id`) REFERENCES `agent`(`id`)
);

CREATE TABLE `task_step` (
    `id` VARCHAR(36) PRIMARY KEY,
    `task_id` VARCHAR(36) NOT NULL,
    `step_index` INT NOT NULL,
    `step_name` VARCHAR(100),
    `step_type` VARCHAR(50) COMMENT 'SEQUENTIAL/PARALLEL/CONDITIONAL',
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `agent_id` VARCHAR(36),
    `input` JSON,
    `output` JSON,
    `started_at` DATETIME,
    `completed_at` DATETIME,
    INDEX `idx_task_id` (`task_id`),
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`agent_id`) REFERENCES `agent`(`id`)
);

CREATE TABLE `task_log` (
    `id` VARCHAR(36) PRIMARY KEY,
    `task_id` VARCHAR(36) NOT NULL,
    `agent_id` VARCHAR(36),
    `level` VARCHAR(10) COMMENT 'INFO/WARN/ERROR/DEBUG',
    `message` TEXT,
    `detail` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`task_id`) REFERENCES `task`(`id`) ON DELETE CASCADE
);
```

### 3.6 cat-orchestration (协同引擎模块)

#### 3.6.1 核心类设计

```java
// 任务分解服务
public interface TaskDecompositionService {
    DecompositionResult decompose(Task task);
    List<TaskStep> generateSteps(Task task);
    boolean canDecompose(Task task);
}

// 任务分配策略
public interface AssignmentStrategy {
    String getName();
    List<Agent> selectAgents(Task task, List<Agent> candidates);
}

// 默认策略实现
@Component
public class CapabilityMatchStrategy implements AssignmentStrategy {
    // 根据能力匹配选择Agent
}

@Component
public class LoadBalanceStrategy implements AssignmentStrategy {
    // 负载均衡策略
}

@Component
public class PriorityStrategy implements AssignmentStrategy {
    // 优先级策略
}

// 协商引擎
public interface NegotiationEngine {
    NegotiationResult executeNegotiation(Task task, List<Agent> agents);
    VoteResult executeVote(Task task, VoteConfig config);
    AuctionResult executeAuction(Task task, List<Agent> agents);
}

// 工作流引擎
public interface WorkflowEngine {
    WorkflowInstance startWorkflow(WorkflowDefinition definition, Task task);
    void executeNextStep(WorkflowInstance instance);
    void handleStepResult(WorkflowInstance instance, StepResult result);
}
```

#### 3.6.2 任务分解策略

```
任务类型分解规则:

1. SIMPLE (简单任务)
   └── 直接分配给一个Agent

2. WORKFLOW (流程化任务)
   ┌───────┐    ┌───────┐    ┌───────┐
   │ Step1 │───▶│ Step2 │───▶│ Step3 │
   └───────┘    └───────┘    └───────┘

3. PARALLEL (并行任务)
         ┌───────┐
      ┌──│ Task1 │──┐
      │  └───────┘  │
   ┌──┴──┐       ┌──┴──┐
   │Task2│       │Task3│
   └──┬──┘       └──┬──┘
      │  ┌───────┐  │
      └──│Merge  │──┘
         └───────┘

4. NEGOTIATION (协商任务)
   ┌───────┐
   │  Init │
   └───┬───┘
       ▼
   ┌───────┐    ┌───────┐
   │Discuss│───▶│ Vote  │
   └───────┘    └───┬───┘
                    ▼
               ┌───────┐
               │ Result│
               └───────┘
```

### 3.7 cat-message (消息服务模块)

#### 3.7.1 核心类设计

```java
// 消息服务
public interface MessageService {
    void sendMessage(Message message);
    void broadcast(Message message);
    void sendToGroup(String groupId, Message message);
    List<Message> getMessages(String agentId, int limit);
    void markAsRead(String messageId);
}

// WebSocket连接管理
public interface WebSocketSessionManager {
    void registerSession(String agentId, WebSocketSession session);
    void removeSession(String agentId);
    void sendToAgent(String agentId, Object payload);
    void broadcast(Object payload);
    Set<String> getOnlineAgents();
}

// 消息队列服务
public interface MessageQueueService {
    void enqueue(String queueName, Object message);
    Object dequeue(String queueName, long timeout);
    void subscribe(String queueName, MessageHandler handler);
}
```

#### 3.7.2 消息协议

```java
// WebSocket消息格式
@Data
public class WebSocketMessage {
    private String type;        // task_assigned, message, command, heartbeat
    private String messageId;
    private Object payload;
    private Long timestamp;
}

// 任务分配消息
@Data
public class TaskAssignedMessage {
    private String taskId;
    private String taskType;
    private Object input;
    private Map<String, Object> config;
    private Long timeout;
}

// Agent消息
@Data
public class AgentMessage {
    private String fromAgentId;
    private String toAgentId;
    private String messageType;
    private Object content;
    private Long timestamp;
}
```

### 3.8 cat-monitor (监控服务模块)

#### 3.8.1 核心类设计

```java
// 系统监控服务
public interface SystemMonitorService {
    SystemMetrics getSystemMetrics();
    List<SystemMetrics> getMetricsHistory(Duration duration);
    void recordMetrics(SystemMetrics metrics);
}

// Agent监控服务
public interface AgentMonitorService {
    AgentMetrics getAgentMetrics(String agentId);
    List<AgentMetrics> getAllAgentMetrics();
    void recordAgentMetric(String agentId, AgentMetric metric);
}

// 任务监控服务
public interface TaskMonitorService {
    TaskStatistics getTaskStatistics();
    List<TaskMetrics> getRecentTaskMetrics(int limit);
}

// 告警服务
public interface AlertService {
    void createAlertRule(AlertRule rule);
    void deleteAlertRule(String ruleId);
    List<AlertRule> getAlertRules();
    List<Alert> getActiveAlerts();
    void acknowledgeAlert(String alertId);
}
```

#### 3.8.2 监控指标

```sql
CREATE TABLE `system_metrics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `metric_time` DATETIME NOT NULL,
    `cpu_usage` DECIMAL(5,2),
    `memory_usage` DECIMAL(5,2),
    `disk_usage` DECIMAL(5,2),
    `active_agents` INT,
    `running_tasks` INT,
    `request_count` INT,
    `avg_response_time` INT,
    INDEX `idx_metric_time` (`metric_time`)
);

CREATE TABLE `alert_rule` (
    `id` VARCHAR(36) PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `metric_type` VARCHAR(50) NOT NULL,
    `condition` VARCHAR(50) NOT NULL COMMENT 'GT/LT/EQ/GE/LE',
    `threshold` DECIMAL(10,2) NOT NULL,
    `duration_seconds` INT DEFAULT 60,
    `severity` VARCHAR(20) DEFAULT 'WARNING' COMMENT 'INFO/WARNING/CRITICAL',
    `notification_channels` JSON,
    `enabled` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_metric_type` (`metric_type`)
);

CREATE TABLE `alert` (
    `id` VARCHAR(36) PRIMARY KEY,
    `rule_id` VARCHAR(36),
    `severity` VARCHAR(20),
    `message` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ACKNOWLEDGED/RESOLVED',
    `triggered_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `resolved_at` DATETIME,
    `acknowledged_by` VARCHAR(36),
    INDEX `idx_status` (`status`),
    INDEX `idx_triggered_at` (`triggered_at`)
);
```

### 3.9 cat-audit (审计服务模块)

#### 3.9.1 核心类设计

```java
// 审计日志服务
public interface AuditLogService {
    void log(AuditEntry entry);
    Page<AuditEntry> queryAuditLogs(AuditQuery query);
    void exportAuditLogs(AuditQuery query, OutputStream output);
}

// 审计切面
@Aspect
@Component
public class AuditAspect {
    @AfterReturning("@annotation(auditLog)")
    public void afterSuccess(JoinPoint joinPoint, AuditLog auditLog) {
        // 记录成功操作
    }

    @AfterThrowing("@annotation(auditLog)")
    public void afterFailure(JoinPoint joinPoint, AuditLog auditLog, Exception e) {
        // 记录失败操作
    }
}
```

#### 3.9.2 数据模型

```sql
CREATE TABLE `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36),
    `username` VARCHAR(50),
    `action` VARCHAR(50) NOT NULL COMMENT 'LOGIN/LOGOUT/CREATE/UPDATE/DELETE/EXECUTE',
    `resource_type` VARCHAR(50) NOT NULL COMMENT 'USER/AGENT/TASK/CONFIG',
    `resource_id` VARCHAR(36),
    `resource_name` VARCHAR(200),
    `detail` JSON,
    `ip_address` VARCHAR(50),
    `user_agent` VARCHAR(500),
    `status` VARCHAR(20) COMMENT 'SUCCESS/FAILURE',
    `error_message` TEXT,
    `duration_ms` INT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action` (`action`),
    INDEX `idx_resource_type` (`resource_type`),
    INDEX `idx_created_at` (`created_at`)
);
```

### 3.10 cat-runtime (Agent运行时模块)

> **Wave 1 更新 (2026-04-04):** 本节已重构为CLI Agent Runtime模块。
> 原有"内置执行器"和"外部Agent适配器"已废弃，统一为CLI进程管理。

#### 3.10.1 CLI进程管理器 (Wave 1)

```java
// CLI进程管理器
@Component
public class CliProcessManager {
    private final Map<String, ProcessHandle> processMap = new ConcurrentHashMap<>();
    private final Map<String, CliSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 启动CLI进程
     */
    public ProcessHandle startProcess(CliAgentConfig config) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(buildCommand(config));
        pb.environment().putAll(config.getEnvVars());
        pb.directory(new File(config.getWorkingDir()));

        Process process = pb.start();
        ProcessHandle handle = new ProcessHandle(
            process.pid(),
            process.getInputStream(),
            process.getOutputStream(),
            process.getErrorStream()
        );

        processMap.put(config.getAgentId(), handle);
        return handle;
    }

    /**
     * 停止CLI进程
     */
    public void stopProcess(String agentId) {
        ProcessHandle handle = processMap.get(agentId);
        if (handle != null) {
            handle.getProcess().destroy();
            processMap.remove(agentId);
        }
    }

    /**
     * 重启CLI进程
     */
    public ProcessHandle restartProcess(String agentId, CliAgentConfig config) {
        stopProcess(agentId);
        return startProcess(config);
    }

    /**
     * 检查进程健康状态
     */
    public boolean isHealthy(String agentId) {
        ProcessHandle handle = processMap.get(agentId);
        return handle != null && handle.getProcess().isAlive();
    }

    /**
     * 构建命令
     */
    private List<String> buildCommand(CliAgentConfig config) {
        List<String> command = new ArrayList<>();
        command.add(config.getExecutablePath());
        command.addAll(config.getArgs());
        return command;
    }
}
```

#### 3.10.2 CLI会话管理器 (Wave 1)

```java
// CLI会话管理器
@Component
public class CliSessionManager {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 建立持久会话
     */
    public CliSession establishSession(String agentId, ProcessHandle processHandle) {
        CliSession session = new CliSession(
            agentId,
            processHandle.getInputStream(),
            processHandle.getOutputStream()
        );
        session.startOutputReader();
        return session;
    }

    /**
     * 发送任务输入
     */
    public void sendTask(String agentId, CliSession session, TaskInput input) {
        String inputJson = objectMapper.writeValueAsString(input);
        session.getOutputStream().write(inputJson.getBytes());
        session.getOutputStream().write('\n');
        session.getOutputStream().flush();
    }

    /**
     * 订阅流式输出
     */
    public Flux<StreamOutput> subscribeOutput(String agentId, CliSession session) {
        return Flux.create(sink -> {
            session.setOutputListener(line -> {
                StreamOutput output = parseOutput(line);
                sink.next(output);
            });
        });
    }

    /**
     * 解析输出
     */
    private StreamOutput parseOutput(String line) {
        // 尝试解析JSON格式
        if (line.startsWith("{")) {
            return objectMapper.readValue(line, StreamOutput.class);
        }
        // 否则作为纯文本
        return new StreamOutput("text", line, null);
    }
}
```

#### 3.10.3 输出解析器 (Wave 1)

```java
// CLI输出解析器接口
public interface CliOutputParser {
    StreamOutput parse(String line);
    TokenUsage parseTokenUsage(String output);
}

// Claude Code输出解析器
@Component
public class ClaudeCodeOutputParser implements CliOutputParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public StreamOutput parse(String line) {
        if (line.startsWith("data: ")) {
            String json = line.substring(6);
            JsonNode node = objectMapper.readTree(json);

            String type = node.get("type").asText();
            String content = node.has("content") ? node.get("content").asText() : null;
            TokenUsage usage = parseTokenUsage(node);

            return new StreamOutput(type, content, usage);
        }
        return new StreamOutput("raw", line, null);
    }

    @Override
    public TokenUsage parseTokenUsage(JsonNode node) {
        if (node.has("usage")) {
            JsonNode usage = node.get("usage");
            return new TokenUsage(
                usage.get("input_tokens").asLong(),
                usage.get("output_tokens").asLong()
            );
        }
        return null;
    }
}

// OpenCode输出解析器
@Component
public class OpenCodeOutputParser implements CliOutputParser {
    private final Pattern tokenPattern = Pattern.compile("tokens:\\s*(\\d+)/(\\d+)");

    @Override
    public StreamOutput parse(String line) {
        // OpenCode特定的输出格式解析
        return new StreamOutput("text", line, null);
    }

    @Override
    public TokenUsage parseTokenUsage(String output) {
        Matcher matcher = tokenPattern.matcher(output);
        if (matcher.find()) {
            return new TokenUsage(
                Long.parseLong(matcher.group(1)),
                Long.parseLong(matcher.group(2))
            );
        }
        return null;
    }
}
```

#### 3.10.4 流式输出WebSocket推送 (Wave 1)

```java
// 流式输出推送服务
@Service
public class StreamOutputPushService {
    private final SimpMessagingTemplate messagingTemplate;
    private final CliSessionManager sessionManager;

    /**
     * 推送CLI输出到前端
     */
    public void pushOutput(String agentId, StreamOutput output) {
        // WebSocket推送到订阅该Agent的前端客户端
        messagingTemplate.convertAndSend(
            "/topic/agent/" + agentId + "/output",
            output
        );
    }

    /**
     * 推送Token使用更新
     */
    public void pushTokenUsage(String agentId, TokenUsage usage) {
        messagingTemplate.convertAndSend(
            "/topic/agent/" + agentId + "/tokens",
            usage
        );
    }

    /**
     * 推送任务进度
     */
    public void pushTaskProgress(String taskId, int progress, String message) {
        messagingTemplate.convertAndSend(
            "/topic/task/" + taskId + "/progress",
            Map.of("progress", progress, "message", message)
        );
    }
}
```

#### 3.10.5 任务执行控制 (Wave 1)

```java
// CLI任务执行控制服务
@Service
public class CliTaskExecutionService {
    private final CliProcessManager processManager;
    private final CliSessionManager sessionManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // 并发控制
    private final Semaphore concurrentLimit = new Semaphore(10);

    /**
     * 执行任务
     */
    public TaskResult executeTask(String agentId, TaskInput input, TaskConfig config) {
        // 并发控制
        concurrentLimit.acquire();

        try {
            CliSession session = sessionManager.getSession(agentId);
            if (session == null) {
                throw new AgentNotRunningException(agentId);
            }

            // 设置超时
            ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
                cancelTask(agentId, input.getTaskId());
            }, config.getTimeoutSeconds(), TimeUnit.SECONDS);

            // 发送任务输入
            sessionManager.sendTask(agentId, session, input);

            // 等待结果
            TaskResult result = waitForResult(session, input.getTaskId(), config);

            // 取消超时任务
            timeoutFuture.cancel(false);

            return result;
        } finally {
            concurrentLimit.release();
        }
    }

    /**
     * 取消任务
     */
    public void cancelTask(String agentId, String taskId) {
        CliSession session = sessionManager.getSession(agentId);
        if (session != null) {
            // 发送取消信号
            sessionManager.sendTask(agentId, session, new CancelSignal(taskId));
        }
    }

    /**
     * 等待结果
     */
    private TaskResult waitForResult(CliSession session, String taskId, TaskConfig config) {
        CompletableFuture<TaskResult> future = new CompletableFuture<>();

        session.setResultListener((resultTaskId, result) -> {
            if (taskId.equals(resultTaskId)) {
                future.complete(result);
            }
        });

        return future.get(config.getTimeoutSeconds(), TimeUnit.SECONDS);
    }
}
```

#### 3.10.6 [DEPRECATED - Wave 1] 原执行器设计

> **废弃说明:** 以下内置执行器和外部适配器设计已废弃。

<details>
<summary>点击查看原执行器设计（已废弃）</summary>

```java
// [DEPRECATED] 执行器接口
public interface AgentExecutor {
    String getCapabilityType();
    ExecutionResult execute(ExecutionContext context);
    boolean validate(ExecutionRequest request);
}

// [DEPRECATED] 命令执行器
@Component
public class CommandExecutor implements AgentExecutor {
    @Override
    public String getCapabilityType() {
        return "COMMAND";
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        // 执行系统命令
        ProcessBuilder pb = new ProcessBuilder(context.getCommand());
        Process process = pb.start();
        // 处理输出
    }
}

// [DEPRECATED] API调用执行器
@Component
public class ApiCallExecutor implements AgentExecutor {
    @Override
    public String getCapabilityType() {
        return "API_CALL";
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        // 使用RestTemplate或WebClient发起HTTP请求
    }
}

// [DEPRECATED] 文件处理执行器
@Component
public class FileExecutor implements AgentExecutor {
    @Override
    public String getCapabilityType() {
        return "FILE";
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        // 文件读写操作
    }
}

// [DEPRECATED] 文本处理执行器
@Component
public class TextExecutor implements AgentExecutor {
    @Override
    public String getCapabilityType() {
        return "TEXT";
    }

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        // 文本处理操作
    }
}
```

```java
// [DEPRECATED] MCP协议适配器
@Component
public class McpProtocolAdapter {
    private final McpClient mcpClient;

    public void registerTools(String agentId, List<McpTool> tools) {
        // 注册MCP工具
    }

    public Object callTool(String agentId, String toolName, Map<String, Object> params) {
        // 调用MCP工具
    }
}

// [DEPRECATED] Skills适配器
@Component
public class SkillsAdapter {
    public void registerSkill(String agentId, SkillDefinition skill) {
        // 注册技能
    }

    public Object executeSkill(String agentId, String skillName, Object input) {
        // 执行技能
    }
}
```

</details>

### 3.11 cat-common (公共模块)

#### 3.11.1 公共组件

```
cat-common/
├── src/main/java/com/cat/common/
│   ├── constant/           # 常量定义
│   │   ├── AgentStatus.java
│   │   ├── TaskStatus.java
│   │   ├── TaskType.java
│   │   └── ErrorCode.java
│   ├── exception/          # 异常定义
│   │   ├── BusinessException.java
│   │   ├── AuthException.java
│   │   └── AgentException.java
│   ├── model/              # 公共模型
│   │   ├── PageRequest.java
│   │   ├── PageResult.java
│   │   └── ApiResponse.java
│   ├── util/               # 工具类
│   │   ├── JsonUtils.java
│   │   ├── DateUtils.java
│   │   └── IdGenerator.java
│   └── config/             # 公共配置
│       ├── RedisConfig.java
│       ├── JacksonConfig.java
│       └── AsyncConfig.java
└── src/main/resources/
    └── application-common.yml
```

---

## 4. API设计

### 4.1 RESTful API规范

#### 4.1.1 URL规范

```
基础路径: /api/v1

资源命名: 小写复数形式，使用连字符
例如: /agents, /tasks, /task-assignments

操作命名:
- 列表: GET /agents
- 详情: GET /agents/{id}
- 创建: POST /agents
- 更新: PUT /agents/{id}
- 删除: DELETE /agents/{id}
- 操作: POST /agents/{id}/actions/{action}
```

#### 4.1.2 请求响应格式

```json
// 成功响应
{
    "code": 0,
    "message": "success",
    "data": {
        // 业务数据
    },
    "timestamp": 1709875200000
}

// 分页响应
{
    "code": 0,
    "message": "success",
    "data": {
        "items": [],
        "total": 100,
        "page": 1,
        "pageSize": 20
    }
}

// 错误响应
{
    "code": 40001,
    "message": "参数错误: name不能为空",
    "data": null,
    "timestamp": 1709875200000
}
```

#### 4.1.3 认证API

```yaml
# 登录
POST /api/v1/auth/login
Request:
  {
    "username": "admin",
    "password": "password123"
  }
Response:
  {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200
  }

# 刷新Token
POST /api/v1/auth/refresh
Request:
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }

# 登出
POST /api/v1/auth/logout

# 获取当前用户信息
GET /api/v1/auth/me
```

#### 4.1.4 Agent管理API (Wave 1 更新)

> **Wave 1 更新 (2026-04-04):** API已重构为CLI Agent模式。

```yaml
# ========== CLI Agent模板API ==========

# 获取内置模板列表
GET /api/v1/cli-agent/templates

Response:
  {
    "code": 0,
    "data": {
      "items": [
        {
          "id": "claude-code",
          "name": "Claude Code",
          "cliType": "claude",
          "description": "Anthropic Claude CLI工具",
          "requiredEnvVars": ["ANTHROPIC_API_KEY"],
          "optionalEnvVars": ["CLAUDE_MODEL", "CLAUDE_MAX_TOKENS"]
        },
        {
          "id": "opencode",
          "name": "OpenCode",
          "cliType": "opencode",
          "description": "开源代码助手CLI",
          "requiredEnvVars": ["OPENAI_API_KEY"],
          "optionalEnvVars": ["OPENAI_MODEL"]
        }
      ]
    }
  }

# 获取模板详情
GET /api/v1/cli-agent/templates/{templateId}

# ========== CLI Agent实例API ==========

# Agent列表
GET /api/v1/cli-agents?page=1&pageSize=20&status=RUNNING

Response:
  {
    "code": 0,
    "data": {
      "items": [
        {
          "id": "agent-001",
          "name": "Claude Code Agent",
          "templateId": "claude-code",
          "templateName": "Claude Code",
          "status": "RUNNING",
          "capabilities": [
            {"type": "CODE_GEN", "domainTags": ["frontend", "vue"]}
          ],
          "metrics": {
            "tasksTotal": 128,
            "successRate": 0.985,
            "totalTokensUsed": 1234567
          },
          "processInfo": {
            "pid": 12345,
            "cpuUsage": 15.5,
            "memoryUsageMb": 256.3
          }
        }
      ],
      "total": 10
    }
  }

# 创建CLI Agent实例
POST /api/v1/cli-agents
Request:
  {
    "name": "Claude Code Agent",
    "description": "主要处理前端代码生成任务",
    "templateId": "claude-code",
    "executablePath": "/usr/local/bin/claude",  # 可选，默认使用模板路径
    "args": ["--model", "claude-sonnet-4-6"],  # 可选，追加参数
    "envVars": {
      "ANTHROPIC_API_KEY": "sk-xxx",  # 必填
      "CLAUDE_MODEL": "claude-sonnet-4-6"  # 可选
    },
    "workingDir": "/home/user/projects",  # 可选
    "capabilities": [
      {"type": "CODE_GEN", "domainTags": ["frontend", "vue", "typescript"], "proficiencyLevel": 4},
      {"type": "FILE_OP", "domainTags": ["read", "write"], "proficiencyLevel": 5}
    ]
  }

# 更新Agent配置
PUT /api/v1/cli-agents/{agentId}
Request:
  {
    "name": "新名称",
    "description": "新描述",
    "args": ["--model", "claude-opus-4-6"],
    "envVars": {
      "ANTHROPIC_API_KEY": "sk-new-key"
    }
  }

# 删除Agent
DELETE /api/v1/cli-agents/{agentId}

# ========== Agent生命周期API ==========

# 启动Agent（启动CLI进程）
POST /api/v1/cli-agents/{agentId}/actions/start

# 停止Agent（停止CLI进程）
POST /api/v1/cli-agents/{agentId}/actions/stop

# 重启Agent
POST /api/v1/cli-agents/{agentId}/actions/restart

# ========== Agent状态与监控API ==========

# 获取Agent详情
GET /api/v1/cli-agents/{agentId}

# 获取Agent状态
GET /api/v1/cli-agents/{agentId}/status

Response:
  {
    "code": 0,
    "data": {
      "agentId": "agent-001",
      "status": "EXECUTING",
      "processId": 12345,
      "currentTaskId": "task-001",
      "startedAt": "2026-04-04T10:00:00Z",
      "uptimeSeconds": 3600,
      "processMetrics": {
        "cpuUsagePercent": 25.5,
        "memoryUsageMb": 512.3
      }
    }
  }

# 获取Agent指标
GET /api/v1/cli-agents/{agentId}/metrics

# 获取Agent输出日志
GET /api/v1/cli-agents/{agentId}/logs?limit=100

# 获取Token使用统计
GET /api/v1/cli-agents/{agentId}/token-usage?range=7d

Response:
  {
    "code": 0,
    "data": {
      "agentId": "agent-001",
      "totalInputTokens": 1234567,
      "totalOutputTokens": 234567,
      "totalTokens": 1469134,
      "estimatedCostUsd": 15.23,
      "dailyStats": [
        {"date": "2026-04-01", "inputTokens": 100000, "outputTokens": 20000},
        {"date": "2026-04-02", "inputTokens": 150000, "outputTokens": 30000}
      ]
    }
  }

# ========== Agent能力API ==========

# 声明Agent能力
POST /api/v1/cli-agents/{agentId}/capabilities
Request:
  {
    "capabilities": [
      {"type": "CODE_GEN", "domainTags": ["frontend", "vue"], "proficiencyLevel": 4}
    ]
  }

# 获取Agent能力列表
GET /api/v1/cli-agents/{agentId}/capabilities

# 根据能力查找Agent
GET /api/v1/cli-agents/match?capabilityType=CODE_GEN&domainTags=frontend,vue

# ========== WebSocket订阅 ==========

# 订阅Agent输出流
# ws://{host}/ws/topic/agent/{agentId}/output

# 订阅Token使用更新
# ws://{host}/ws/topic/agent/{agentId}/tokens

# 订阅任务进度
# ws://{host}/ws/topic/task/{taskId}/progress
```

#### 4.1.5 [DEPRECATED - Wave 1] 原Agent管理API

<details>
<summary>点击查看原Agent管理API（已废弃）</summary>

```yaml
# [DEPRECATED] Agent列表
GET /api/v1/agents?page=1&pageSize=20&type=BUILT_IN&status=ONLINE

# [DEPRECATED] Agent详情
GET /api/v1/agents/{agentId}

# [DEPRECATED] 创建内置Agent
POST /api/v1/agents
Request:
  {
    "name": "数据采集Agent",
    "description": "负责数据采集任务",
    "type": "BUILT_IN",
    "capabilities": [
      {"type": "API_CALL", "name": "HTTP请求"},
      {"type": "FILE", "name": "文件读写"}
    ]
  }

# [DEPRECATED] 更新Agent
PUT /api/v1/agents/{agentId}

# [DEPRECATED] 删除Agent
DELETE /api/v1/agents/{agentId}

# [DEPRECATED] 启用/禁用Agent
POST /api/v1/agents/{agentId}/actions/enable
POST /api/v1/agents/{agentId}/actions/disable

# [DEPRECATED] 获取Agent接入凭证
GET /api/v1/agents/{agentId}/credential

# [DEPRECATED] Agent心跳
POST /api/v1/agents/{agentId}/heartbeat

# [DEPRECATED] Agent获取待执行任务
GET /api/v1/agents/{agentId}/tasks?status=PENDING

# [DEPRECATED] Agent提交任务结果
POST /api/v1/agents/{agentId}/tasks/{taskId}/result
```

</details>

#### 4.1.5 任务管理API

```yaml
# 任务列表
GET /api/v1/tasks?page=1&pageSize=20&type=WORKFLOW&status=RUNNING

# 任务详情
GET /api/v1/tasks/{taskId}

# 创建任务
POST /api/v1/tasks
Request:
  {
    "name": "数据采集任务",
    "type": "WORKFLOW",
    "priority": 1,
    "input": {
      "url": "https://api.example.com/data",
      "format": "json"
    },
    "config": {
      "timeout": 3600,
      "retryCount": 3
    }
  }

# 更新任务
PUT /api/v1/tasks/{taskId}

# 取消任务
POST /api/v1/tasks/{taskId}/actions/cancel

# 删除任务
DELETE /api/v1/tasks/{taskId}

# 获取任务日志
GET /api/v1/tasks/{taskId}/logs

# 获取任务结果
GET /api/v1/tasks/{taskId}/result

# 手动分配Agent
POST /api/v1/tasks/{taskId}/actions/assign
Request:
  {
    "agentIds": ["agent-001", "agent-002"]
  }
```

### 4.2 WebSocket协议

#### 4.2.1 连接地址

```
ws://{host}/ws/agent/{agentId}?token={jwtToken}
```

#### 4.2.2 消息类型

```json
// 服务端 -> Agent: 任务分配
{
    "type": "TASK_ASSIGNED",
    "messageId": "msg-001",
    "payload": {
        "taskId": "task-001",
        "taskType": "SIMPLE",
        "input": {},
        "config": {},
        "timeout": 3600
    },
    "timestamp": 1709875200000
}

// 服务端 -> Agent: 消息通知
{
    "type": "MESSAGE",
    "messageId": "msg-002",
    "payload": {
        "fromAgentId": "agent-002",
        "messageType": "TEXT",
        "content": "请等待我完成"
    },
    "timestamp": 1709875200000
}

// Agent -> 服务端: 心跳
{
    "type": "HEARTBEAT",
    "messageId": "msg-003",
    "payload": {
        "status": "BUSY",
        "progress": 60
    },
    "timestamp": 1709875200000
}

// Agent -> 服务端: 任务进度
{
    "type": "TASK_PROGRESS",
    "messageId": "msg-004",
    "payload": {
        "taskId": "task-001",
        "progress": 50,
        "message": "正在处理数据..."
    },
    "timestamp": 1709875200000
}

// Agent -> 服务端: 任务完成
{
    "type": "TASK_COMPLETED",
    "messageId": "msg-005",
    "payload": {
        "taskId": "task-001",
        "status": "SUCCESS",
        "output": {},
        "duration": 1500
    },
    "timestamp": 1709875200000
}
```

### 4.3 gRPC接口

#### 4.3.1 Proto定义

```protobuf
syntax = "proto3";

package cat.agent.v1;

option java_multiple_files = true;
option java_package = "com.cat.grpc.agent.v1";

// Agent服务
service AgentService {
    rpc RegisterAgent(RegisterAgentRequest) returns (RegisterAgentResponse);
    rpc Heartbeat(HeartbeatRequest) returns (HeartbeatResponse);
    rpc GetTask(GetTaskRequest) returns (GetTaskResponse);
    rpc SubmitResult(SubmitResultRequest) returns (SubmitResultResponse);
    rpc StreamTask(stream TaskProgress) returns (stream TaskAssignment);
}

// 消息服务
service MessageService {
    rpc SendMessage(SendMessageRequest) returns (SendMessageResponse);
    rpc Subscribe(SubscribeRequest) returns (stream Message);
}

message RegisterAgentRequest {
    string name = 1;
    string type = 2;
    repeated Capability capabilities = 3;
}

message Capability {
    string type = 1;
    string name = 2;
    map<string, string> config = 3;
}

message TaskAssignment {
    string task_id = 1;
    string task_type = 2;
    map<string, string> input = 3;
    int64 timeout_seconds = 4;
}

message TaskProgress {
    string task_id = 1;
    int32 progress = 2;
    string message = 3;
    map<string, string> metrics = 4;
}
```

---

## 5. 前端架构

### 5.1 项目结构

```
cat-web/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                    # API请求
│   │   ├── auth.ts
│   │   ├── agent.ts
│   │   ├── task.ts
│   │   └── monitor.ts
│   ├── assets/                 # 静态资源
│   │   ├── images/
│   │   └── styles/
│   │       ├── variables.scss
│   │       ├── mixins.scss
│   │       └── global.scss
│   ├── components/             # 公共组件
│   │   ├── layout/
│   │   │   ├── AppLayout.vue
│   │   │   ├── Sidebar.vue
│   │   │   └── Header.vue
│   │   ├── common/
│   │   │   ├── CatButton.vue
│   │   │   ├── CatCard.vue
│   │   │   ├── CatTag.vue
│   │   │   └── CatModal.vue
│   │   ├── agent/
│   │   │   ├── AgentCard.vue
│   │   │   ├── AgentForm.vue
│   │   │   └── AgentStatusTag.vue
│   │   └── task/
│   │       ├── TaskCard.vue
│   │       ├── TaskForm.vue
│   │       └── TaskProgress.vue
│   ├── composables/            # 组合式函数
│   │   ├── useAuth.ts
│   │   ├── useAgent.ts
│   │   ├── useTask.ts
│   │   └── useWebSocket.ts
│   ├── router/                 # 路由
│   │   └── index.ts
│   ├── stores/                 # 状态管理(Pinia)
│   │   ├── auth.ts
│   │   ├── agent.ts
│   │   └── task.ts
│   ├── types/                  # 类型定义
│   │   ├── api.ts
│   │   ├── agent.ts
│   │   └── task.ts
│   ├── utils/                  # 工具函数
│   │   ├── request.ts
│   │   ├── storage.ts
│   │   └── format.ts
│   ├── views/                  # 页面视图
│   │   ├── login/
│   │   │   └── LoginView.vue
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue
│   │   ├── agent/
│   │   │   ├── AgentListView.vue
│   │   │   └── AgentDetailView.vue
│   │   ├── task/
│   │   │   ├── TaskListView.vue
│   │   │   └── TaskDetailView.vue
│   │   ├── monitor/
│   │   │   └── MonitorView.vue
│   │   └── settings/
│   │       └── SettingsView.vue
│   ├── App.vue
│   └── main.ts
├── .env
├── .env.development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

### 5.2 核心配置

#### 5.2.1 Vite配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
})
```

#### 5.2.2 路由配置

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/views/agent/AgentListView.vue')
      },
      {
        path: 'agents/:id',
        name: 'AgentDetail',
        component: () => import('@/views/agent/AgentDetailView.vue')
      },
      {
        path: 'tasks',
        name: 'TaskList',
        component: () => import('@/views/task/TaskListView.vue')
      },
      {
        path: 'tasks/:id',
        name: 'TaskDetail',
        component: () => import('@/views/task/TaskDetailView.vue')
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/MonitorView.vue')
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/SettingsView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.path === '/login' && authStore.isAuthenticated) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
```

#### 5.2.3 API请求封装

```typescript
// utils/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { message } from '@/utils/message'

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message: msg, data } = response.data
    if (code === 0) {
      return data
    } else {
      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    } else {
      message.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export const request = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config)
  }
}
```

### 5.3 组件示例

#### 5.3.1 Agent卡片组件

```vue
<!-- components/agent/AgentCard.vue -->
<template>
  <div class="agent-card" @click="$emit('click')">
    <div class="agent-card__header">
      <div class="agent-card__avatar">
        <CatIcon name="cat-face" :size="40" />
      </div>
      <div class="agent-card__info">
        <h3 class="agent-card__name">{{ agent.name }}</h3>
        <p class="agent-card__desc">{{ agent.description }}</p>
      </div>
      <AgentStatusTag :status="agent.status" />
    </div>

    <div class="agent-card__capabilities">
      <CatTag
        v-for="cap in agent.capabilities"
        :key="cap.type"
        size="small"
        type="capability"
      >
        {{ cap.name }}
      </CatTag>
    </div>

    <div class="agent-card__stats">
      <div class="stat-item">
        <span class="stat-value">{{ agent.metrics?.tasksTotal || 0 }}</span>
        <span class="stat-label">执行任务</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ successRate }}%</span>
        <span class="stat-label">成功率</span>
      </div>
    </div>

    <div class="agent-card__actions">
      <CatButton size="small" @click.stop="$emit('detail')">详情</CatButton>
      <CatButton size="small" type="outline" @click.stop="$emit('config')">配置</CatButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Agent } from '@/types/agent'
import AgentStatusTag from './AgentStatusTag.vue'
import CatTag from '@/components/common/CatTag.vue'
import CatButton from '@/components/common/CatButton.vue'

const props = defineProps<{
  agent: Agent
}>()

defineEmits<{
  click: []
  detail: []
  config: []
}>()

const successRate = computed(() => {
  if (!props.agent.metrics?.tasksTotal) return 0
  return ((props.agent.metrics.tasksSuccess / props.agent.metrics.tasksTotal) * 100).toFixed(1)
})
</script>

<style lang="scss" scoped>
.agent-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
  border: 1px solid #f0e6d8;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 8px 24px rgba(139, 115, 85, 0.12);
    border-color: #FFB366;
    transform: translateY(-2px);
  }

  &__header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: linear-gradient(135deg, #FF8C42 0%, #FFB366 100%);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__info {
    flex: 1;
  }

  &__name {
    font-size: 16px;
    font-weight: 600;
    color: #262626;
    margin: 0 0 4px 0;
  }

  &__desc {
    font-size: 13px;
    color: #8C8C8C;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__capabilities {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
  }

  &__stats {
    display: flex;
    gap: 24px;
    padding: 12px 0;
    border-top: 1px solid #f0e6d8;
    border-bottom: 1px solid #f0e6d8;
    margin-bottom: 16px;
  }

  &__actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

.stat-item {
  display: flex;
  flex-direction: column;

  .stat-value {
    font-size: 18px;
    font-weight: 600;
    color: #FF8C42;
  }

  .stat-label {
    font-size: 12px;
    color: #8C8C8C;
  }
}
</style>
```

---

## 6. 安全设计

### 6.1 认证授权

#### 6.1.1 JWT认证

```java
@Configuration
public class JwtConfig {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:7200}")
    private Long expiration;

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(secret, expiration);
    }
}

@Component
public class JwtTokenProvider {
    private final String secret;
    private final Long expiration;
    private final Key key;

    public JwtTokenProvider(String secret, Long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

#### 6.1.2 权限控制

```java
// 权限注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasPermission(#resource, #action)")
public @interface RequirePermission {
    String resource();
    String action();
}

// 权限评估器
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {
    @Autowired
    private RoleService roleService;

    @Override
    public boolean hasPermission(Authentication authentication, Object resource, Object action) {
        String userId = ((UserDetails) authentication.getPrincipal()).getUsername();
        String permissionCode = resource + ":" + action;
        return roleService.hasPermission(userId, permissionCode);
    }
}
```

### 6.2 Agent认证

```java
// Agent认证拦截器
@Component
public class AgentAuthInterceptor implements HandlerInterceptor {
    @Autowired
    private ExternalAgentService externalAgentService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String agentId = request.getHeader("X-Agent-Id");
        String accessKey = request.getHeader("X-Agent-Key");

        if (agentId == null || accessKey == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        AgentCredential credential = externalAgentService.validateCredential(accessKey);
        if (credential == null || !credential.getAgentId().equals(agentId)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        request.setAttribute("agentId", agentId);
        return true;
    }
}
```

### 6.3 数据安全

- **密码存储**: 使用BCrypt加密
- **敏感数据**: 数据库字段加密
- **通信安全**: 强制HTTPS
- **日志脱敏**: 敏感信息脱敏处理

---

## 7. 部署架构

### 7.1 Docker部署

#### 7.1.1 Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: cat_agent
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init/sql:/docker-entrypoint-initdb.d
    ports:
      - "3306:3306"
    networks:
      - cat-network

  redis:
    image: redis:7.0-alpine
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    networks:
      - cat-network

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    networks:
      - cat-network

  cat-gateway:
    build: ./cat-gateway
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cat_agent
      SPRING_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - mysql
      - redis
      - rabbitmq
    networks:
      - cat-network

  cat-auth:
    build: ./cat-auth
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cat_agent
      SPRING_REDIS_HOST: redis
    depends_on:
      - mysql
      - redis
    networks:
      - cat-network

  cat-agent:
    build: ./cat-agent
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cat_agent
      SPRING_REDIS_HOST: redis
    depends_on:
      - mysql
      - redis
    networks:
      - cat-network

  cat-task:
    build: ./cat-task
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/cat_agent
      SPRING_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      - mysql
      - redis
      - rabbitmq
    networks:
      - cat-network

  cat-web:
    build: ./cat-web
    ports:
      - "3000:80"
    depends_on:
      - cat-gateway
    networks:
      - cat-network

volumes:
  mysql_data:
  redis_data:
  rabbitmq_data:

networks:
  cat-network:
    driver: bridge
```

### 7.2 生产部署架构

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    │      (Nginx)    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
        ┌─────────┐    ┌─────────┐    ┌─────────┐
        │ cat-web │    │ cat-web │    │ cat-web │
        │   (1)   │    │   (2)   │    │   (3)   │
        └────┬────┘    └────┬────┘    └────┬────┘
             │              │              │
             └──────────────┼──────────────┘
                            │
                    ┌───────┴───────┐
                    │  API Gateway  │
                    │   (Cluster)   │
                    └───────┬───────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
   ┌──────────┐       ┌──────────┐       ┌──────────┐
   │ Services │       │ Services │       │ Services │
   │ Cluster  │       │ Cluster  │       │ Cluster  │
   └─────┬────┘       └─────┬────┘       └─────┬────┘
         │                  │                  │
         └──────────────────┼──────────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
   ┌──────────┐       ┌──────────┐       ┌──────────┐
   │  MySQL   │       │  Redis   │       │ RabbitMQ │
   │ Primary  │       │ Cluster  │       │ Cluster  │
   │ + Slave  │       │          │       │          │
   └──────────┘       └──────────┘       └──────────┘
```

---

## 8. 监控运维

### 8.1 健康检查

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @GetMapping("/health")
    public Health health() {
        return Health.up()
            .withDetail("service", "cat-agent-platform")
            .withDetail("version", "1.0.0")
            .build();
    }
}
```

### 8.2 日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"cat-agent-platform"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

---

## 9. 开发计划

### 9.1 开发阶段

| 阶段 | 内容 | 交付物 |
|------|------|--------|
| Phase 1 | 核心功能开发 | Agent管理、任务管理、基础协同 |
| Phase 2 | 安全与监控 | 认证授权、审计日志、监控告警 |
| Phase 3 | 高级功能 | 高级协同、协议适配、性能优化 |

### 9.2 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| WebSocket连接数限制 | 高 | 使用Redis发布订阅支持集群 |
| 任务调度性能 | 中 | 使用分布式调度框架 |
| Agent状态同步 | 中 | 优化心跳机制，使用Redis缓存 |

---

**文档审批**

| 角色 | 姓名 | 签名 | 日期 |
|------|------|------|------|
| 架构师 | | | |
| 技术负责人 | | | |
| 项目经理 | | | |