# Git Worktree 多Agent并发协同开发功能

**文档编号:** FEATURE-WORKTREE-001
**版本:** 1.0.0
**日期:** 2026-04-14
**状态:** 已实现

---

## 1. 功能概述

### 1.1 背景

在多Agent协同开发场景中，多个AI Agent需要并发处理同一项目的不同任务（如Feature开发、Bug修复等）。如果多个Agent共用同一个工作目录，会产生文件冲突、互相覆盖、状态紊乱等问题。

### 1.2 方案选择

经过技术调研（参考主流方案：Git Branch、Git Worktree、Container隔离、多Clone），最终选择 **Git Worktree** 方案，原因如下：

| 评估维度 | Git Worktree ⭐ | 多Clone | 容器隔离 |
|---------|-----------------|---------|---------|
| 与现有架构兼容性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| 实现复杂度 | 低 | 中 | 高 |
| 资源开销 | 低（共享.git） | 中 | 高 |
| 隔离程度 | 文件系统级 | 仓库级 | OS级 |
| 创建/销毁速度 | 秒级 | 分钟级 | 分钟级 |
| 外部依赖 | 无（仅Git） | 无 | Docker |

### 1.3 核心原理

```
Git Worktree 允许同一个仓库同时拥有多个工作目录，
每个工作目录检出不同的分支，共享同一个 .git 目录。

/projects/my-repo/                    ← 主仓库 (main)
    .git/                             ← 共享Git元数据
    .worktrees/                       ← worktree存放目录
        ws-abc12345-feature-login/    ← Agent-1 的工作空间
        ws-def67890-fix-bug-456/      ← Agent-2 的工作空间
        ws-ghi24680-refactor-api/     ← Agent-3 的工作空间
```

---

## 2. 架构设计

### 2.1 整体架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                      Cat Agent Platform                              │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │                  WorkspaceController (REST API)                │  │
│  │  POST /api/v1/workspaces           创建工作空间                │  │
│  │  GET  /api/v1/workspaces           列出工作空间                │  │
│  │  GET  /api/v1/workspaces/{id}      获取工作空间详情            │  │
│  │  DELETE /api/v1/workspaces/{id}    删除工作空间                │  │
│  │  GET  /api/v1/workspaces/{id}/git-status    获取Git状态        │  │
│  │  POST /api/v1/workspaces/{id}/commit        提交变更           │  │
│  │  POST /api/v1/workspaces/{id}/push          推送分支           │  │
│  │  POST /api/v1/workspaces/{id}/merge         合并分支           │  │
│  │  POST /api/v1/workspaces/{id}/check-conflicts 冲突检测         │  │
│  │  POST /api/v1/workspaces/{id}/sync          同步主分支         │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                           │                                          │
│                           ▼                                          │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │              WorkspaceService (接口)                            │  │
│  │              LocalWorkspaceService (实现)                       │  │
│  │                                                               │  │
│  │  - createWorkspace()      创建worktree + 分支                 │  │
│  │  - removeWorkspace()      移除worktree + 删除分支             │  │
│  │  - getWorkspaceGitStatus() 获取Git状态                        │  │
│  │  - commitChanges()        git add + git commit                │  │
│  │  - pushBranch()           git push                            │  │
│  │  - mergeBranch()          git merge                           │  │
│  │  - checkConflicts()       合并冲突预检                         │  │
│  │  - syncFromBranch()       git rebase 同步主分支               │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                           │                                          │
│                           ▼                                          │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │              StoredWorkspace (JSON存储)                         │  │
│  │              workspaces.json                                   │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 工作流程

```
┌──────────┐     ┌───────────────────┐     ┌──────────────────┐
│  创建任务  │────▶│  创建Workspace     │────▶│  Agent在独立     │
│  分配Agent │     │  (worktree+branch) │     │  worktree中执行  │
└──────────┘     └───────────────────┘     └────────┬─────────┘
                                                     │
     ┌──────────────────┐     ┌──────────────────┐   │
     │  清理Worktree     │◀────│  提交 → 推送 →   │◀──┘
     │  (可选)           │     │  合并/创建PR      │
     └──────────────────┘     └──────────────────┘
```

### 2.3 冲突避免策略

```
┌─────────────────────────────────────────────────────────────┐
│                  冲突避免机制                                  │
│                                                             │
│  1. 工作空间隔离（Worktree）                                  │
│     - 每个Agent拥有独立的文件系统工作目录                       │
│     - 文件修改互不影响                                        │
│                                                             │
│  2. 分支隔离                                                 │
│     - 每个工作空间对应独立分支                                  │
│     - 分支名包含taskId和agentId确保唯一                        │
│                                                             │
│  3. 合并前冲突预检                                            │
│     - checkConflicts API 在合并前检测是否存在冲突               │
│     - 存在冲突时阻止自动合并，通知人工介入                       │
│                                                             │
│  4. 同步机制                                                 │
│     - syncFromBranch API 支持将主分支变更rebase到工作空间       │
│     - 定期同步减少最终合并时的冲突概率                           │
│                                                             │
│  5. 顺序合并策略                                              │
│     - 先完成的任务先合并                                       │
│     - 后续任务合并前需同步最新主分支                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 新增/修改文件清单

### 3.1 新增文件

| 文件路径 | 描述 |
|---------|------|
| `cat-standalone/src/main/java/com/cat/cliagent/service/WorkspaceService.java` | 工作空间服务接口 - 定义worktree生命周期管理和Git操作的完整API |
| `cat-standalone/src/main/java/com/cat/standalone/service/LocalWorkspaceService.java` | 工作空间服务实现 - 通过git CLI命令实现worktree的创建、删除、提交、推送、合并等 |
| `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredWorkspace.java` | 工作空间存储实体 - 持久化worktree的状态信息 |
| `cat-standalone/src/main/java/com/cat/standalone/controller/WorkspaceController.java` | 工作空间REST控制器 - 提供完整的工作空间管理API |
| `cat-web/src/api/workspace.ts` | 前端工作空间API模块 - TypeScript类型定义和API调用封装 |
| `cat-web/src/views/workspace/WorkspaceListView.vue` | 前端工作空间管理视图 - 创建、查看状态、提交、推送、合并、删除 |
| `docs/features/git-worktree-feature.md` | 本文档 - Git Worktree功能设计说明 |

### 3.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `cat-standalone/src/main/java/com/cat/standalone/store/StoreConfig.java` | 新增 `workspaceStore` Bean 注册 |
| `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTask.java` | 新增 `projectPath`、`branchName`、`worktreePath`、`workspaceId` 字段 |
| `cat-standalone/src/main/java/com/cat/standalone/store/entity/StoredTaskAssignment.java` | 新增 `worktreePath`、`branchName`、`workspaceId` 字段 |
| `cat-standalone/src/main/java/com/cat/standalone/service/LocalCliTaskExecutionService.java` | 新增 `executeTaskWithWorkspace()` 方法，支持在独立worktree中执行任务 |
| `cat-standalone/src/main/java/com/cat/standalone/controller/CliAgentController.java` | 新增 `executeTaskWithWorkspace` 端点和 `WorkspaceTaskExecuteRequest` DTO |
| `cat-web/src/router/index.ts` | 新增 `/workspaces` 路由 |
| `CLAUDE.md` | 更新项目架构说明，新增Workspace相关内容 |
| `feature-list.json` | 新增工作空间管理功能条目 |

---

## 4. API 参考

### 4.1 创建工作空间

```
POST /api/v1/workspaces
Content-Type: application/json

{
  "projectPath": "/path/to/project",
  "branchName": "feature/my-task",      // 可选，自动生成
  "baseBranch": "main",                 // 可选，默认 main
  "taskId": "task-uuid",                // 可选
  "agentId": "agent-uuid",              // 可选
  "description": "实现登录功能"          // 可选
}
```

### 4.2 获取Git状态

```
GET /api/v1/workspaces/{id}/git-status

Response:
{
  "workspaceId": "abc12345",
  "branchName": "feature/my-task",
  "currentCommit": "a1b2c3d4...",
  "modifiedFiles": 3,
  "stagedFiles": 1,
  "untrackedFiles": 2,
  "hasUncommittedChanges": true,
  "aheadCount": 2,
  "behindCount": 0,
  "modifiedFileList": ["src/App.vue", "src/api/task.ts", ...],
  "untrackedFileList": ["src/new-file.ts", ...]
}
```

### 4.3 提交变更

```
POST /api/v1/workspaces/{id}/commit
Content-Type: application/json

{
  "message": "feat: implement login feature"
}
```

### 4.4 合并分支

```
POST /api/v1/workspaces/{id}/merge
Content-Type: application/json

{
  "targetBranch": "main"
}
```

### 4.5 冲突检测

```
POST /api/v1/workspaces/{id}/check-conflicts
Content-Type: application/json

{
  "targetBranch": "main"
}
```

### 4.6 使用工作空间执行任务

```
POST /api/v1/cli-agents/{agentId}/tasks/execute-with-workspace
Content-Type: application/json

{
  "input": "请实现用户登录功能",
  "timeoutSeconds": 300,
  "projectPath": "/path/to/project",
  "baseBranch": "main",
  "description": "实现登录功能"
}
```

---

## 5. 配置参数

| 参数 | 默认值 | 描述 |
|------|--------|------|
| `cat.workspace.worktree-dir-name` | `.worktrees` | worktree存放目录名称 |
| `cat.workspace.default-base-branch` | `main` | 默认基线分支 |

---

## 6. 使用场景示例

### 6.1 场景：3个Agent并发处理不同任务

```
用户创建3个任务：
  - 任务1：实现登录功能 → Agent-A
  - 任务2：修复#456 Bug → Agent-B  
  - 任务3：重构API模块 → Agent-C

平台自动创建3个工作空间：
  workspace-1: branch=task/task1/agent-A, path=.worktrees/ws-xxx-...
  workspace-2: branch=task/task2/agent-B, path=.worktrees/ws-yyy-...
  workspace-3: branch=task/task3/agent-C, path=.worktrees/ws-zzz-...

3个Agent并发执行，互不干扰。

Agent-B最先完成 → 提交 → 推送 → 合并到main
Agent-A随后完成 → 同步main → 提交 → 推送 → 合并到main
Agent-C最后完成 → 同步main → 冲突检测 → 解决冲突 → 合并到main

所有worktree清理完毕。
```

---

## 7. 数据存储

工作空间数据存储在 `./data/workspaces.json`：

```json
{
  "abc12345": {
    "id": "abc12345",
    "projectPath": "/path/to/project",
    "worktreePath": "/path/to/project/.worktrees/ws-abc12345-feature-login",
    "branchName": "task/task1/agent-A/abc12345",
    "baseBranch": "main",
    "status": "ACTIVE",
    "taskId": "task-uuid-1",
    "agentId": "agent-uuid-A",
    "description": "实现登录功能",
    "lastCommitHash": null,
    "lastCommitMessage": null,
    "createdAt": "2026-04-14T16:00:00",
    "updatedAt": "2026-04-14T16:00:00"
  }
}
```

### 工作空间状态流转

```
ACTIVE → COMMITTED → MERGED → REMOVED
   │                    │
   └──→ ERROR          └──→ REMOVED
```

---

## 8. 前端页面

访问路径：`http://localhost:3000/workspaces`

功能：
- 创建工作空间（指定项目路径、分支名、关联任务/Agent）
- 查看工作空间列表（支持按状态、项目路径过滤）
- 查看Git状态（修改文件、ahead/behind等）
- 提交变更（git add + commit）
- 推送分支（git push）
- 合并分支（含冲突预检）
- 同步主分支（git rebase）
- 删除工作空间（移除worktree + 分支）
