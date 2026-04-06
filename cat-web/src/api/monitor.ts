import { request } from '@/utils/request'

// ===== Types =====

export interface SystemMetrics {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  activeAgents: number
  runningTasks: number
  requestCount: number
  avgResponseTime: number
  timestamp: number
}

export interface TaskStatistics {
  totalCount: number
  successCount: number
  failedCount: number
  successRate: number
  avgDurationMs: number
}

export interface AlertRule {
  id: string
  name: string
  metricType: string
  condition: 'GT' | 'LT' | 'EQ' | 'GE' | 'LE'
  threshold: number
  durationSeconds: number
  severity: 'INFO' | 'WARNING' | 'CRITICAL'
  notificationChannels?: string
  enabled: boolean
  createdAt: string
}

export interface Alert {
  id: string
  ruleId: string
  severity: 'INFO' | 'WARNING' | 'CRITICAL'
  message: string
  status: 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED'
  triggeredAt: string
  resolvedAt?: string
  acknowledgedBy?: string
  detail?: string
}

export interface CreateAlertRuleRequest {
  name: string
  metricType: string
  condition: 'GT' | 'LT' | 'EQ' | 'GE' | 'LE'
  threshold: number
  durationSeconds?: number
  severity?: 'INFO' | 'WARNING' | 'CRITICAL'
  notificationChannels?: Array<{
    type: string
    target: string
  }>
  enabled?: boolean
}

export interface AlertListParams {
  page?: number
  pageSize?: number
  status?: string
  severity?: string
}

export interface AlertRuleListParams {
  page?: number
  pageSize?: number
  metricType?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  pageSize: number
  page: number
  totalPages: number
}

// ===== 系统监控 API =====

/**
 * 获取当前系统指标
 */
export function getCurrentMetrics(): Promise<SystemMetrics> {
  return request.get('/monitor/metrics')
}

/**
 * 获取历史指标
 */
export function getMetricsHistory(params: {
  startTime: string
  endTime: string
  interval?: string
}): Promise<SystemMetrics[]> {
  return request.get('/monitor/metrics/history', { params })
}

/**
 * 获取任务统计
 */
export function getTaskStatistics(): Promise<TaskStatistics> {
  return request.get('/monitor/tasks/statistics')
}

/**
 * 获取Agent状态分布
 */
export function getAgentStatusDistribution(): Promise<{
  online: number
  offline: number
  busy: number
  error: number
  disabled: number
}> {
  return request.get('/monitor/agents/distribution')
}

// ===== 告警规则 API =====

/**
 * 获取告警规则列表
 */
export function getAlertRules(params: AlertRuleListParams): Promise<PageResult<AlertRule>> {
  return request.get('/alerts/rules', { params })
}

/**
 * 获取告警规则详情
 */
export function getAlertRule(id: string): Promise<AlertRule> {
  return request.get(`/alerts/rules/${id}`)
}

/**
 * 创建告警规则
 */
export function createAlertRule(data: CreateAlertRuleRequest): Promise<AlertRule> {
  return request.post('/alerts/rules', data)
}

/**
 * 更新告警规则
 */
export function updateAlertRule(id: string, data: Partial<CreateAlertRuleRequest>): Promise<AlertRule> {
  return request.put(`/alerts/rules/${id}`, data)
}

/**
 * 删除告警规则
 */
export function deleteAlertRule(id: string): Promise<void> {
  return request.delete(`/alerts/rules/${id}`)
}

/**
 * 启用/禁用告警规则
 */
export function toggleAlertRule(id: string, enabled: boolean): Promise<void> {
  return request.put(`/alerts/rules/${id}/toggle`, null, { params: { enabled } })
}

// ===== 告警记录 API =====

/**
 * 获取告警列表
 */
export function getAlerts(params: AlertListParams): Promise<PageResult<Alert>> {
  return request.get('/alerts', { params })
}

/**
 * 获取告警详情
 */
export function getAlert(id: string): Promise<Alert> {
  return request.get(`/alerts/${id}`)
}

/**
 * 确认告警
 */
export function acknowledgeAlert(id: string, acknowledgedBy: string): Promise<void> {
  return request.put(`/alerts/${id}/acknowledge`, null, { params: { acknowledgedBy } })
}

/**
 * 解决告警
 */
export function resolveAlert(id: string): Promise<void> {
  return request.put(`/alerts/${id}/resolve`)
}

/**
 * 批量解决告警
 */
export function resolveAlerts(ids: string[]): Promise<void> {
  return request.put('/alerts/resolve', ids)
}

/**
 * 获取活跃告警统计
 */
export function getActiveAlertCounts(): Promise<{
  ACTIVE: number
  ACKNOWLEDGED: number
  CRITICAL: number
}> {
  return request.get('/alerts/counts')
}

/**
 * 手动触发告警检查
 */
export function runAlertChecks(): Promise<void> {
  return request.post('/alerts/check')
}