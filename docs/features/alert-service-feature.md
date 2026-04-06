# 告警服务实现文档

**特性编号:** FEAT-018
**模块名称:** cat-monitor
**实现日期:** 2026-04-02
**状态:** 已完成

---

## 1. 概述

实现完整的告警服务，支持：
- 自定义告警规则配置
- 多种指标类型监控
- 多种条件判断
- 持续时间检查
- 多级别告警
- 告警生命周期管理
- 定时自动检查

---

## 2. 核心组件

### 2.1 实体类

| 类名 | 路径 | 描述 |
|------|------|------|
| AlertRule | entity/AlertRule.java | 告警规则实体 |
| Alert | entity/Alert.java | 告警记录实体 |

### 2.2 Mapper

| 类名 | 路径 | 描述 |
|------|------|------|
| AlertRuleMapper | mapper/AlertRuleMapper.java | 告警规则Mapper |
| AlertMapper | mapper/AlertMapper.java | 告警记录Mapper |

### 2.3 服务层

| 类名 | 路径 | 描述 |
|------|------|------|
| AlertService | service/AlertService.java | 告警服务接口 |
| AlertServiceImpl | service/impl/AlertServiceImpl.java | 告警服务实现 |

### 2.4 控制器

| 类名 | 路径 | 描述 |
|------|------|------|
| AlertController | controller/AlertController.java | 告警API控制器 |

### 2.5 DTO

| 类名 | 路径 | 描述 |
|------|------|------|
| AlertRuleRequest | dto/AlertRuleRequest.java | 规则创建/更新请求 |

---

## 3. 告警规则配置

### 3.1 支持的指标类型

| 类型 | 编码 | 描述 |
|------|------|------|
| CPU使用率 | CPU_USAGE | 系统CPU使用率百分比 |
| 内存使用率 | MEMORY_USAGE | 系统内存使用率百分比 |
| 磁盘使用率 | DISK_USAGE | 磁盘使用率百分比 |
| Agent离线 | AGENT_OFFLINE | 离线Agent数量 |
| 任务失败率 | TASK_FAILURE_RATE | 任务失败率百分比 |
| 响应时间 | RESPONSE_TIME | API响应时间(ms) |
| 活跃连接数 | ACTIVE_CONNECTIONS | WebSocket活跃连接数 |

### 3.2 条件类型

| 条件 | 编码 | 描述 |
|------|------|------|
| 大于 | GT | value > threshold |
| 小于 | LT | value < threshold |
| 等于 | EQ | value == threshold |
| 大于等于 | GE | value >= threshold |
| 小于等于 | LE | value <= threshold |

### 3.3 严重级别

| 级别 | 编码 | 描述 |
|------|------|------|
| 信息 | INFO | 信息提示 |
| 警告 | WARNING | 需要关注 |
| 严重 | CRITICAL | 需要立即处理 |

---

## 4. 告警生命周期

```
┌─────────────────────────────────────────────────────────────────┐
│                      告警生命周期                               │
└─────────────────────────────────────────────────────────────────┘

1. 指标监控
   │
   ├── 定时检查(每分钟)
   │   │
   │   ├── 获取指标值
   │   │
   │   └─▶ 2. 条件判断

       2. 条件判断
       │
       ├── 条件满足 ─▶ 记录触发时间
       │               │
       │               ├── 持续时间 >= 配置 ─▶ 3. 触发告警
       │               │
       │               └── 持续时间 < 配置 ─▶ 继续监控
       │
       └── 条件不满足 ─▶ 重置状态，自动解决告警

           3. 触发告警
           │
           ├── 状态: ACTIVE
           ├── 发送通知
           │
           └─▶ 4. 等待处理

               4. 处理告警
               │
               ├── 确认 ─▶ 状态: ACKNOWLEDGED
               │           记录确认人、时间
               │
               └── 解决 ─▶ 状态: RESOLVED
                           记录解决时间
```

---

## 5. API接口

### 5.1 告警规则管理

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/alerts/rules | GET | 分页查询规则 |
| /api/v1/alerts/rules | POST | 创建规则 |
| /api/v1/alerts/rules/{ruleId} | GET | 获取规则详情 |
| /api/v1/alerts/rules/{ruleId} | PUT | 更新规则 |
| /api/v1/alerts/rules/{ruleId} | DELETE | 删除规则 |
| /api/v1/alerts/rules/enabled | GET | 获取所有启用的规则 |
| /api/v1/alerts/rules/{ruleId}/toggle | PUT | 启用/禁用规则 |

### 5.2 告警记录管理

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/alerts | GET | 分页查询告警 |
| /api/v1/alerts/{alertId} | GET | 获取告警详情 |
| /api/v1/alerts/{alertId}/acknowledge | PUT | 确认告警 |
| /api/v1/alerts/{alertId}/resolve | PUT | 解决告警 |
| /api/v1/alerts/resolve | PUT | 批量解决告警 |
| /api/v1/alerts/counts | GET | 获取活跃告警统计 |
| /api/v1/alerts/check | POST | 手动触发检查 |

---

## 6. 使用示例

### 6.1 创建CPU使用率告警规则

```json
POST /api/v1/alerts/rules
{
  "name": "CPU使用率过高告警",
  "metricType": "CPU_USAGE",
  "condition": "GT",
  "threshold": 80,
  "durationSeconds": 60,
  "severity": "WARNING",
  "notificationChannels": [
    {
      "type": "WEBSOCKET",
      "target": "admin"
    }
  ],
  "enabled": true
}
```

### 6.2 创建Agent离线告警规则

```json
POST /api/v1/alerts/rules
{
  "name": "Agent离线告警",
  "metricType": "AGENT_OFFLINE",
  "condition": "GT",
  "threshold": 0,
  "durationSeconds": 30,
  "severity": "CRITICAL",
  "notificationChannels": [
    {
      "type": "WEBSOCKET",
      "target": "all"
    }
  ],
  "enabled": true
}
```

### 6.3 确认告警

```
PUT /api/v1/alerts/{alertId}/acknowledge?acknowledgedBy=admin
```

### 6.4 解决告警

```
PUT /api/v1/alerts/{alertId}/resolve
```

---

## 7. 定时检查

AlertServiceImpl中的`runAlertChecks()`方法每分钟自动执行：
- 检查CPU使用率
- 检查内存使用率
- 检查Agent离线数量
- 检查任务失败率

可通过`@Scheduled(fixedRate = 60000)`配置调整检查频率。

---

## 8. 通知渠道

支持的通知类型：
- **WEBSOCKET** - 通过WebSocket推送
- **EMAIL** - 邮件通知(待实现)
- **SMS** - 短信通知(待实现)
- **WEBHOOK** - HTTP回调(待实现)

---

## 9. 特性验收状态

### FEAT-018: 告警服务

| 验收标准 | 状态 |
|----------|------|
| 支持自定义告警规则 | ✓ 已实现 |
| 告警及时触发 | ✓ 定时检查，每分钟 |
| 告警记录可查询 | ✓ 分页查询API |
| 支持告警确认 | ✓ acknowledge接口 |
| 持续时间检查 | ✓ 已实现 |
| 多种条件判断 | ✓ GT/LT/EQ/GE/LE |
| 多级别告警 | ✓ INFO/WARNING/CRITICAL |

---

## 10. 数据库表结构

### alert_rule表

| 字段 | 类型 | 描述 |
|------|------|------|
| id | VARCHAR(36) | 规则ID |
| name | VARCHAR(100) | 规则名称 |
| metric_type | VARCHAR(50) | 指标类型 |
| condition | VARCHAR(50) | 条件 |
| threshold | DECIMAL(10,2) | 阈值 |
| duration_seconds | INT | 持续时间(秒) |
| severity | VARCHAR(20) | 严重级别 |
| notification_channels | JSON | 通知渠道 |
| enabled | TINYINT | 是否启用 |
| created_at | DATETIME | 创建时间 |

### alert表

| 字段 | 类型 | 描述 |
|------|------|------|
| id | VARCHAR(36) | 告警ID |
| rule_id | VARCHAR(36) | 规则ID |
| severity | VARCHAR(20) | 严重级别 |
| message | VARCHAR(500) | 告警消息 |
| status | VARCHAR(20) | 状态 |
| triggered_at | DATETIME | 触发时间 |
| resolved_at | DATETIME | 解决时间 |
| acknowledged_by | VARCHAR(36) | 确认人 |
| detail | JSON | 详情 |

---

## 11. 测试建议

1. 创建各类告警规则测试
2. 模拟指标超阈值触发告警
3. 测试持续时间检查
4. 测试告警确认和解决
5. 测试自动解决机制
6. 测试分页查询功能