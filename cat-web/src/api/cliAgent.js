// CLI Agent API
import request from '@/utils/request'

// 模板相关API

/**
 * 获取所有CLI Agent模板
 */
export function getTemplates() {
  return request.get('/cli-agent/templates')
}

/**
 * 获取内置模板
 */
export function getBuiltInTemplates() {
  return request.get('/cli-agent/templates/built-in')
}

/**
 * 获取模板详情
 */
export function getTemplate(id) {
  return request.get(`/cli-agent/templates/${id}`)
}

/**
 * 创建自定义模板
 */
export function createTemplate(data) {
  return request.post('/cli-agent/templates', data)
}

/**
 * 更新模板
 */
export function updateTemplate(id, data) {
  return request.put(`/cli-agent/templates/${id}`, data)
}

/**
 * 删除模板
 */
export function deleteTemplate(id) {
  return request.delete(`/cli-agent/templates/${id}`)
}

// Agent实例相关API

/**
 * 获取CLI Agent列表
 */
export function getAgents(params) {
  return request.get('/cli-agents', { params })
}

/**
 * 获取Agent详情
 */
export function getAgent(id) {
  return request.get(`/cli-agents/${id}`)
}

/**
 * 创建Agent
 */
export function createAgent(data) {
  return request.post('/cli-agents', data)
}

/**
 * 更新Agent
 */
export function updateAgent(id, data) {
  return request.put(`/cli-agents/${id}`, data)
}

/**
 * 删除Agent
 */
export function deleteAgent(id) {
  return request.delete(`/cli-agents/${id}`)
}

/**
 * 获取可用Agent列表
 */
export function getAvailableAgents() {
  return request.get('/cli-agents/available')
}

// 进程生命周期API

/**
 * 启动Agent进程
 */
export function startAgent(id) {
  return request.post(`/cli-agents/${id}/actions/start`)
}

/**
 * 停止Agent进程
 */
export function stopAgent(id) {
  return request.post(`/cli-agents/${id}/actions/stop`)
}

/**
 * 重启Agent进程
 */
export function restartAgent(id) {
  return request.post(`/cli-agents/${id}/actions/restart`)
}

/**
 * 获取进程状态
 */
export function getAgentStatus(id) {
  return request.get(`/cli-agents/${id}/status`)
}

/**
 * 检查进程健康
 */
export function checkAgentHealth(id) {
  return request.get(`/cli-agents/${id}/health`)
}

// 会话通信API

/**
 * 发送输入到CLI
 */
export function sendInput(id, input) {
  return request.post(`/cli-agents/${id}/session/input`, input, {
    headers: { 'Content-Type': 'text/plain' }
  })
}

/**
 * 获取会话状态
 */
export function getSessionStatus(id) {
  return request.get(`/cli-agents/${id}/session/status`)
}

/**
 * 关闭会话
 */
export function closeSession(id) {
  return request.post(`/cli-agents/${id}/session/close`)
}

/**
 * 获取输出日志
 */
export function getOutputLogs(id, limit = 50) {
  return request.get(`/cli-agents/${id}/logs`, { params: { limit } })
}

/**
 * 清空输出日志
 */
export function clearOutputLogs(id) {
  return request.post(`/cli-agents/${id}/logs/clear`)
}

// 任务执行API

/**
 * 执行任务
 */
export function executeTask(id, data) {
  return request.post(`/cli-agents/${id}/tasks/execute`, data)
}

/**
 * 取消任务
 */
export function cancelTask(taskId) {
  return request.post(`/cli-agents/tasks/${taskId}/cancel`)
}

/**
 * 获取任务状态
 */
export function getTaskStatus(taskId) {
  return request.get(`/cli-agents/tasks/${taskId}/status`)
}

// Token统计API

/**
 * 获取Agent Token统计
 */
export function getAgentTokenStats(id) {
  return request.get(`/cli-agents/${id}/token-stats`)
}

/**
 * 获取系统Token统计
 */
export function getSystemTokenStats(params) {
  return request.get('/cli-agents/system/token-stats', { params })
}

// 监控API

/**
 * 获取Agent监控状态
 */
export function getAgentMonitorStatus(id) {
  return request.get(`/cli-agents/monitor/${id}`)
}

/**
 * 获取系统概览
 */
export function getSystemOverview() {
  return request.get('/cli-agents/monitor/overview')
}

// 能力管理API

/**
 * 获取Agent能力列表
 */
export function getAgentCapabilities(id) {
  return request.get(`/cli-agents/${id}/capabilities`)
}

/**
 * 添加Agent能力
 */
export function addCapability(id, data) {
  return request.post(`/cli-agents/${id}/capabilities`, data)
}

/**
 * 按能力查找Agent
 */
export function findAgentsByCapability(params) {
  return request.get('/cli-agents/by-capability', { params })
}

/**
 * 获取所有能力类型
 */
export function getCapabilityTypes() {
  return request.get('/cli-agents/capability-types')
}

// 消息通信API

/**
 * 发送消息
 */
export function sendMessage(data) {
  return request.post('/cli-agents/messages/send', data)
}

/**
 * 获取待处理消息
 */
export function getPendingMessages(id) {
  return request.get(`/cli-agents/messages/${id}/pending`)
}