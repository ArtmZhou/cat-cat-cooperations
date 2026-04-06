# WorkflowEngine 完善实现文档

**特性编号:** FEAT-010
**模块名称:** cat-orchestration
**实现日期:** 2026-04-02
**状态:** 已完善

---

## 1. 概述

完善WorkflowEngine的步骤执行逻辑，实现：
- 实际调用Agent执行器执行步骤
- 步骤间数据传递
- 失败重试机制
- Redis持久化存储

---

## 2. 新增组件

### 2.1 ExecutorRegistry

**路径:** `executor/ExecutorRegistry.java`

执行器注册表，管理所有Agent执行器：
- COMMAND - 命令执行器
- API_CALL - API调用执行器
- FILE - 文件处理执行器
- TEXT - 文本处理执行器
- MCP_SKILL - MCP协议适配器

### 2.2 WorkflowEngineImpl 完善

**路径:** `engine/WorkflowEngineImpl.java`

完整实现：
- `startWorkflow()` - 启动工作流，存储到Redis
- `executeNextStep()` - 执行步骤，调用执行器
- `handleStepSuccess()` - 处理步骤成功
- `handleStepFailureWithRetry()` - 处理失败重试
- `handleStepFailure()` - 处理失败终止
- `completeWorkflow()` - 完成工作流

---

## 3. 工作流执行流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                      WorkflowEngine执行流程                          │
└─────────────────────────────────────────────────────────────────────┘

1. startWorkflow(definition, taskId, context)
   │
   ├── 创建WorkflowInstance
   ├── 设置第一个步骤ID
   ├── 存储到Redis
   │
   └─▶ 2. executeNextStep(instance, definition)

       2. executeNextStep()
       │
       ├── 检查状态(RUNNING)
       ├── 查找当前步骤定义
       ├── 创建StepResult记录
       ├── 构建ExecutionContext
       ├── 获取AgentExecutor
       ├── 验证执行请求
       │
       └─▶ 3. executor.execute(context)

           3. 执行结果处理
           │
           ├── 成功 ─▶ handleStepSuccess()
           │           │
           │           ├── 更新StepResult(COMPLETED)
           │           ├── 输出存入context
           │           ├── 查找下一步
           │           │
           │           └─▶ executeNextStep(next) 或 completeWorkflow()
           │
           └── 失败 ─▶ handleStepFailureWithRetry()
                       │
                       ├── 检查重试次数
                       │
                       ├── retryCount < maxRetry
                       │   │
                       │   └─▶ 重试 executeNextStep()
                       │
                       └── retryCount >= maxRetry
                           │
                           ├── onFailure = SKIP ─▶ 跳过，继续下一步
                           │
                           └── onFailure = STOP ─▶ handleStepFailure()
                                                   │
                                                   └─▶ 状态设为FAILED
```

---

## 4. 步骤执行上下文

ExecutionContext构建：
```java
ExecutionContext context = new ExecutionContext();
context.setTaskId(instance.getTaskId());
context.setAgentId(step.getAgentId());
context.setExecutionType(step.getCapabilityType());
context.setParams(合并step.input和workflowContext);
context.setConfig(step.config);
context.setTimeoutMs(config.timeoutMs);
context.setWorkingDirectory(config.workingDirectory);
```

---

## 5. 步骤配置

WorkflowStep支持配置：

| 配置项 | 类型 | 描述 |
|--------|------|------|
| maxRetry | Integer | 最大重试次数，默认3 |
| timeoutMs | Long | 超时时间(毫秒) |
| workingDirectory | String | 工作目录 |
| onFailure | String | 失败策略: SKIP/STOP |

---

## 6. Redis存储结构

| Key | 类型 | 描述 |
|-----|------|------|
| cat:workflow:instance:{instanceId} | WorkflowInstance | 工作流实例 |
| cat:workflow:instance:{instanceId}:definition | WorkflowDefinition | 工作流定义 |
| cat:workflow:task:{taskId} | String | 任务关联的实例ID |

---

## 7. 失败处理策略

| 策略 | 编码 | 描述 |
|------|------|------|
| 跳过继续 | SKIP | 步骤失败后跳过，继续下一步 |
| 终止工作流 | STOP | 步骤失败后终止整个工作流 |

---

## 8. 步骤间数据传递

步骤输出自动存储到工作流context：
```java
instance.getContext().put("step_" + stepId + "_output", output);
```

后续步骤可通过context获取前序步骤输出：
```java
Object prevOutput = instance.getContext().get("step_prevStepId_output");
```

---

## 9. 依赖更新

pom.xml新增依赖：
- cat-agent - Agent信息和能力查询
- cat-runtime - 执行器接口和模型
- spring-boot-starter-data-redis - Redis存储

---

## 10. 特性验收状态

### FEAT-010: 流程化任务编排

| 验收标准 | 状态 |
|----------|------|
| 支持创建多步骤任务 | ✓ 已实现 |
| 步骤按顺序执行 | ✓ 已实现 |
| 步骤间可传递数据 | ✓ 已实现，通过context |
| 支持步骤失败重试 | ✓ 已实现，maxRetry配置 |
| 支持失败跳过策略 | ✓ 已实现，onFailure=SKIP |

---

## 11. 使用示例

```java
// 创建工作流定义
WorkflowDefinition definition = new WorkflowDefinition();
definition.setId("wf-data-processing");
definition.setName("数据处理流程");

List<WorkflowStep> steps = new ArrayList<>();

// 步骤1: 获取数据
WorkflowStep step1 = new WorkflowStep();
step1.setId("step-fetch");
step1.setName("获取数据");
step1.setAgentId("agent-api-001");
step1.setCapabilityType("API_CALL");
step1.setInput(Map.of("url", "https://api.example.com/data"));
steps.add(step1);

// 步骤2: 处理数据
WorkflowStep step2 = new WorkflowStep();
step2.setId("step-process");
step2.setName("处理数据");
step2.setAgentId("agent-text-001");
step2.setCapabilityType("TEXT");
step2.setInput("${step_fetch_output}"); // 使用上一步输出
step2.setConfig(Map.of("maxRetry", 3, "timeoutMs", 30000));
steps.add(step2);

// 步骤3: 保存结果
WorkflowStep step3 = new WorkflowStep();
step3.setId("step-save");
step3.setName("保存结果");
step3.setAgentId("agent-file-001");
step3.setCapabilityType("FILE");
step3.setInput(Map.of("path", "/data/result.json"));
steps.add(step3);

definition.setSteps(steps);

// 启动工作流
WorkflowInstance instance = workflowEngine.startWorkflow(
    definition,
    "task-001",
    Map.of("source", "api")
);
```

---

## 12. 测试建议

1. 单步骤工作流执行测试
2. 多步骤顺序执行测试
3. 步骤间数据传递测试
4. 失败重试测试
5. 失败跳过策略测试
6. 工作流取消测试
7. Redis持久化验证