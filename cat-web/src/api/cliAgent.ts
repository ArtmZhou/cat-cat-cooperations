// CLI Agent API
import request from '@/utils/request'

// ---------- Type Definitions ----------

export interface CreateAgentPayload {
  name: string
  description?: string
  templateId: string
  executablePath?: string
  configPath?: string
  args?: string[]
  workingDir?: string
  envVars?: Record<string, string>
  capabilities?: Array<{ type: string; domainTags: string[]; proficiencyLevel: number }>
}

export interface UpdateAgentPayload {
  name?: string
  description?: string
  executablePath?: string
  configPath?: string
  args?: string[]
  workingDir?: string
  envVars?: Record<string, string>
  capabilities?: Array<{ type: string; domainTags: string[]; proficiencyLevel: number }>
}

export interface AgentQueryParams {
  page?: number
  pageSize?: number
  status?: string
  templateId?: string
  name?: string
}

export interface CapabilityPayload {
  type: string
  domainTags?: string[]
  proficiencyLevel?: number
}

export interface FindByCapabilityParams {
  type?: string
  domain?: string
  minProficiency?: number
}

export interface SystemTokenStatsParams {
  startDate?: string
  endDate?: string
  groupBy?: string
}

export interface TaskPayload {
  task: string
  sessionId?: string
  args?: string[]
}

export interface MessagePayload {
  from?: string
  to?: string
  content: string
  messageType?: string
}

// ---------- Template APIs ----------

/** 获取所有CLI Agent模板 */
export function getTemplates(): Promise<any> {
  return request.get('/cli-agent/templates')
}

/** 获取内置模板 */
export function getBuiltInTemplates(): Promise<any> {
  return request.get('/cli-agent/templates/built-in')
}

/** 获取模板详情 */
export function getTemplate(id: string): Promise<any> {
  return request.get(`/cli-agent/templates/${id}`)
}

/** 创建自定义模板 */
export function createTemplate(data: Record<string, unknown>): Promise<any> {
  return request.post('/cli-agent/templates', data)
}

/** 更新模板 */
export function updateTemplate(id: string, data: Record<string, unknown>): Promise<any> {
  return request.put(`/cli-agent/templates/${id}`, data)
}

/** 删除模板 */
export function deleteTemplate(id: string): Promise<void> {
  return request.delete(`/cli-agent/templates/${id}`)
}

// ---------- Agent Instance APIs ----------

/** 获取CLI Agent列表 */
export function getAgents(params?: AgentQueryParams): Promise<any> {
  return request.get('/cli-agents', { params })
}

/** 获取Agent详情 */
export function getAgent(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}`)
}

/** 创建Agent */
export function createAgent(data: CreateAgentPayload): Promise<any> {
  return request.post('/cli-agents', data)
}

/** 更新Agent */
export function updateAgent(id: string, data: UpdateAgentPayload): Promise<any> {
  return request.put(`/cli-agents/${id}`, data)
}

/** 删除Agent */
export function deleteAgent(id: string): Promise<void> {
  return request.delete(`/cli-agents/${id}`)
}

/** 获取可用Agent列表 */
export function getAvailableAgents(): Promise<any> {
  return request.get('/cli-agents/available')
}

// ---------- Process Lifecycle APIs ----------

/** 启动Agent进程 */
export function startAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/start`)
}

/** 停止Agent进程 */
export function stopAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/stop`)
}

/** 重启Agent进程 */
export function restartAgent(id: string): Promise<any> {
  return request.post(`/cli-agents/${id}/actions/restart`)
}

/** 获取进程状态 */
export function getAgentStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/status`)
}

/** 检查进程健康 */
export function checkAgentHealth(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/health`)
}

// ---------- Session & Communication APIs ----------

/** 发送输入到CLI */
export function sendInput(id: string, input: string): Promise<any> {
  return request.post(`/cli-agents/${id}/session/input`, input, {
    headers: { 'Content-Type': 'text/plain' }
  })
}

/** 获取会话状态 */
export function getSessionStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/session/status`)
}

/** 关闭会话 */
export function closeSession(id: string): Promise<void> {
  return request.post(`/cli-agents/${id}/session/close`)
}

/** 获取输出日志 */
export function getOutputLogs(id: string, limit = 50): Promise<any> {
  return request.get(`/cli-agents/${id}/logs`, { params: { limit } })
}

/** 清空输出日志 */
export function clearOutputLogs(id: string): Promise<void> {
  return request.post(`/cli-agents/${id}/logs/clear`)
}

// ---------- Task Execution APIs ----------

/** 执行任务 */
export function executeTask(id: string, data: TaskPayload): Promise<any> {
  return request.post(`/cli-agents/${id}/tasks/execute`, data)
}

/** 取消任务 */
export function cancelTask(taskId: string): Promise<void> {
  return request.post(`/cli-agents/tasks/${taskId}/cancel`)
}

/** 获取任务状态 */
export function getTaskStatus(taskId: string): Promise<any> {
  return request.get(`/cli-agents/tasks/${taskId}/status`)
}

// ---------- Token Statistics APIs ----------

/** 获取Agent Token统计 */
export function getAgentTokenStats(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/token-stats`)
}

/** 获取系统Token统计 */
export function getSystemTokenStats(params?: SystemTokenStatsParams): Promise<any> {
  return request.get('/cli-agents/system/token-stats', { params })
}

// ---------- Monitoring APIs ----------

/** 获取Agent监控状态 */
export function getAgentMonitorStatus(id: string): Promise<any> {
  return request.get(`/cli-agents/monitor/${id}`)
}

/** 获取系统概览 */
export function getSystemOverview(): Promise<any> {
  return request.get('/cli-agents/monitor/overview')
}

// ---------- Capability Management APIs ----------

/** 获取Agent能力列表 */
export function getAgentCapabilities(id: string): Promise<any> {
  return request.get(`/cli-agents/${id}/capabilities`)
}

/** 添加Agent能力 */
export function addCapability(id: string, data: CapabilityPayload): Promise<any> {
  return request.post(`/cli-agents/${id}/capabilities`, data)
}

/** 按能力查找Agent */
export function findAgentsByCapability(params?: FindByCapabilityParams): Promise<any> {
  return request.get('/cli-agents/by-capability', { params })
}

/** 获取所有能力类型 */
export function getCapabilityTypes(): Promise<any> {
  return request.get('/cli-agents/capability-types')
}

// ---------- Message Communication APIs ----------

/** 发送消息 */
export function sendMessage(data: MessagePayload): Promise<any> {
  return request.post('/cli-agents/messages/send', data)
}

/** 获取待处理消息 */
export function getPendingMessages(id: string): Promise<any> {
  return request.get(`/cli-agents/messages/${id}/pending`)
}
