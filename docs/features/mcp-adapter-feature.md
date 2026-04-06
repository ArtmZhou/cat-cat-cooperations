# MCP协议适配器实现文档

**特性编号:** FEAT-023
**模块名称:** cat-runtime
**实现日期:** 2026-04-02
**状态:** 已完成

---

## 1. 概述

实现MCP (Model Context Protocol) 协议适配器，支持：
- MCP工具注册和管理
- MCP工具调用
- MCP资源注册和访问
- 调用日志记录

---

## 2. 核心组件

### 2.1 模型类

| 类名 | 路径 | 描述 |
|------|------|------|
| McpTool | mcp/McpTool.java | MCP工具定义 |
| McpResource | mcp/McpResource.java | MCP资源定义 |
| McpCallLog | mcp/McpCallLog.java | MCP调用日志 |

### 2.2 服务类

| 类名 | 路径 | 描述 |
|------|------|------|
| McpService | mcp/McpService.java | MCP核心服务 |
| McpExecutor | executor/McpExecutor.java | AgentExecutor实现 |

### 2.3 控制器

| 类名 | 路径 | 描述 |
|------|------|------|
| McpController | controller/McpController.java | MCP管理API |

---

## 3. MCP工具调用流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      MCP工具调用流程                             │
└─────────────────────────────────────────────────────────────────┘

1. 工具注册
   │
   ├── registerTool(tool, handler)
   │   │
   │   ├── 存储工具定义
   │   └── 存储处理器
   │
   └─▶ 工具可用于调用

2. 工具调用
   │
   ├── McpExecutor.execute(context)
   │   │
   │   ├── 解析操作类型
   │   │   ├── call_tool
   │   │   ├── access_resource
   │   │   ├── list_tools
   │   │   └── list_resources
   │   │
   │   └─▶ McpService.callTool(agentId, toolName, input)
   │
   └─▶ 3. 执行处理器

       3. 处理执行
       │
       ├── 查找工具处理器
       │   │
       │   ├── 有处理器 ─▶ handler.handle(input)
       │   │
       │   └── 无处理器 ─▶ 默认处理
       │
       └─▶ 4. 记录日志

           4. 日志记录
           │
           └── McpCallLog
               ├── callType
               ├── targetName
               ├── input/output
               ├── success
               └── durationMs
```

---

## 4. 执行操作类型

| 操作 | 编码 | 描述 | 必需参数 |
|------|------|------|----------|
| 调用工具 | call_tool | 调用指定的MCP工具 | toolName, input |
| 访问资源 | access_resource | 访问指定的MCP资源 | uri |
| 列出工具 | list_tools | 列出所有启用的工具 | - |
| 列出资源 | list_resources | 列出所有资源 | - |
| 获取统计 | get_stats | 获取MCP统计信息 | - |

---

## 5. API接口

### 5.1 工具管理

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/mcp/tools | GET | 获取所有工具 |
| /api/v1/mcp/tools/enabled | GET | 获取启用的工具 |
| /api/v1/mcp/tools/{toolName} | GET | 获取工具详情 |
| /api/v1/mcp/tools | POST | 注册工具 |
| /api/v1/mcp/tools/{toolName} | DELETE | 注销工具 |
| /api/v1/mcp/tools/{toolName}/invoke | POST | 调用工具 |

### 5.2 资源管理

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/mcp/resources | GET | 获取所有资源 |
| /api/v1/mcp/resources | POST | 注册资源 |
| /api/v1/mcp/resources | DELETE | 注销资源 |
| /api/v1/mcp/resources/{uri}/access | GET | 访问资源 |

### 5.3 日志和统计

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/mcp/logs | GET | 获取调用日志 |
| /api/v1/mcp/stats | GET | 获取统计信息 |

---

## 6. 使用示例

### 6.1 注册工具

```java
McpTool tool = new McpTool();
tool.setName("weather_query");
tool.setDescription("Query weather information");
tool.setInputSchema(Map.of(
    "type", "object",
    "properties", Map.of(
        "city", Map.of("type", "string", "description", "City name")
    ),
    "required", List.of("city")
));

mcpService.registerTool(tool, input -> {
    String city = (String) input.get("city");
    // 实际查询逻辑
    return Map.of("city", city, "temperature", "25°C", "weather", "Sunny");
});
```

### 6.2 调用工具（通过Executor）

```java
ExecutionContext context = new ExecutionContext();
context.setAgentId("agent-001");
context.setExecutionType("MCP_SKILL");

Map<String, Object> params = new HashMap<>();
params.put("operation", "call_tool");
params.put("toolName", "weather_query");
params.put("input", Map.of("city", "Beijing"));
context.setParams(params);

ExecutionResult result = mcpExecutor.execute(context);
```

### 6.3 调用工具（通过API）

```bash
POST /api/v1/mcp/tools/weather_query/invoke?agentId=agent-001
Content-Type: application/json

{
  "city": "Beijing"
}
```

### 6.4 注册资源

```java
McpResource resource = new McpResource();
resource.setUri("config://app-settings");
resource.setName("Application Settings");
resource.setDescription("Application configuration settings");
resource.setMimeType("application/json");
resource.setContent(Map.of(
    "appName", "Cat Agent Platform",
    "version", "1.0.0"
));

mcpService.registerResource(resource);
```

---

## 7. 内置工具

系统启动时自动注册以下内置工具：

| 工具名 | 描述 | 参数 |
|--------|------|------|
| echo | 回显输入消息 | message |
| get_time | 获取当前时间 | - |

---

## 8. 执行器集成

McpExecutor实现了AgentExecutor接口，可通过WorkflowEngine调用：

```java
// WorkflowStep配置
WorkflowStep step = new WorkflowStep();
step.setId("step-mcp");
step.setName("MCP工具调用");
step.setAgentId("agent-001");
step.setCapabilityType("MCP_SKILL");
step.setInput(Map.of(
    "operation", "call_tool",
    "toolName", "weather_query",
    "input", Map.of("city", "Beijing")
));
```

---

## 9. 调用日志

每次工具调用和资源访问都会记录日志：

```java
McpCallLog log = new McpCallLog();
log.setId("uuid");
log.setAgentId("agent-001");
log.setCallType("TOOL_CALL");
log.setTargetName("weather_query");
log.setInput(Map.of("city", "Beijing"));
log.setOutput(Map.of("temperature", "25°C"));
log.setSuccess(true);
log.setDurationMs(150);
log.setCalledAt(LocalDateTime.now());
```

日志存储在内存中，最多保留1000条记录。

---

## 10. 特性验收状态

### FEAT-023: MCP协议适配器

| 验收标准 | 状态 |
|----------|------|
| 支持MCP工具注册 | ✓ 已实现 |
| 支持MCP工具调用 | ✓ 已实现 |
| 支持MCP资源访问 | ✓ 已实现 |
| 记录调用日志 | ✓ 已实现 |
| 集成AgentExecutor | ✓ McpExecutor实现 |

---

## 11. 扩展说明

### 11.1 添加自定义工具处理器

```java
@Component
public class MyMcpTools {

    @Autowired
    private McpService mcpService;

    @PostConstruct
    public void init() {
        McpTool tool = new McpTool();
        tool.setName("my_tool");
        tool.setDescription("My custom tool");

        mcpService.registerTool(tool, input -> {
            // 自定义处理逻辑
            return result;
        });
    }
}
```

### 11.2 集成外部MCP服务

可以通过实现McpToolHandler来集成外部MCP服务：

```java
mcpService.registerTool(tool, input -> {
    // 调用外部MCP服务
    return externalMcpClient.call(tool.getName(), input);
});
```

---

## 12. 测试建议

1. 工具注册和注销测试
2. 工具调用参数验证测试
3. 资源访问测试
4. 调用日志记录测试
5. 错误处理测试
6. 并发调用测试