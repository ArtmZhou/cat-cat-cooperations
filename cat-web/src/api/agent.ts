import { request } from '@/utils/request'

// ===== Types =====

export interface Agent {
  id: string
  name: string
  description?: string
  type: 'BUILT_IN' | 'EXTERNAL'
  status: 'OFFLINE' | 'ONLINE' | 'BUSY' | 'ERROR' | 'DISABLED'
  accessKey?: string
  config?: Record<string, unknown>
  metadata?: Record<string, unknown>
  lastHeartbeat?: string
  createdBy?: string
  createdAt: string
  updatedAt: string
}

export interface AgentCapability {
  id: string
  agentId: string
  capabilityType: 'COMMAND' | 'API_CALL' | 'FILE' | 'TEXT' | 'MCP_SKILL'
  capabilityName: string
  capabilityConfig?: Record<string, unknown>
  description?: string
  createdAt: string
}

export interface CreateAgentRequest {
  name: string
  description?: string
  type: 'BUILT_IN' | 'EXTERNAL'
  config?: Record<string, unknown>
  metadata?: Record<string, unknown>
}

export interface UpdateAgentRequest {
  name?: string
  description?: string
  config?: Record<string, unknown>
  metadata?: Record<string, unknown>
}

export interface AgentListParams {
  page?: number
  pageSize?: number
  status?: string
  type?: string
  name?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  pageSize: number
  page: number
  totalPages: number
}

// ===== API Functions =====

/**
 * 获取Agent列表
 */
export function getAgentList(params: AgentListParams): Promise<PageResult<Agent>> {
  return request.get('/agents', { params })
}

/**
 * 获取Agent详情
 */
export function getAgent(id: string): Promise<Agent> {
  return request.get(`/agents/${id}`)
}

/**
 * 创建Agent
 */
export function createAgent(data: CreateAgentRequest): Promise<Agent> {
  return request.post('/agents', data)
}

/**
 * 更新Agent
 */
export function updateAgent(id: string, data: UpdateAgentRequest): Promise<Agent> {
  return request.put(`/agents/${id}`, data)
}

/**
 * 删除Agent
 */
export function deleteAgent(id: string): Promise<void> {
  return request.delete(`/agents/${id}`)
}

/**
 * 启用Agent
 */
export function enableAgent(id: string): Promise<void> {
  return request.post(`/agents/${id}/actions/enable`)
}

/**
 * 禁用Agent
 */
export function disableAgent(id: string): Promise<void> {
  return request.post(`/agents/${id}/actions/disable`)
}

/**
 * 获取Agent能力列表
 */
export function getAgentCapabilities(agentId: string): Promise<AgentCapability[]> {
  return request.get(`/agents/${agentId}/capabilities`)
}

/**
 * 注册Agent能力
 */
export function registerCapability(agentId: string, data: Partial<AgentCapability>): Promise<AgentCapability> {
  return request.post(`/agents/${agentId}/capabilities`, data)
}

/**
 * 删除Agent能力
 */
export function deleteCapability(agentId: string, capabilityId: string): Promise<void> {
  return request.delete(`/agents/${agentId}/capabilities/${capabilityId}`)
}

/**
 * 生成Agent接入凭证
 */
export function generateAccessKey(agentId: string): Promise<{ accessKey: string }> {
  return request.post(`/agents/${agentId}/access-key`)
}

/**
 * 撤销Agent接入凭证
 */
export function revokeAccessKey(agentId: string): Promise<void> {
  return request.delete(`/agents/${agentId}/access-key`)
}

/**
 * Agent心跳
 */
export function heartbeat(agentId: string): Promise<void> {
  return request.post(`/agents/${agentId}/heartbeat`)
}

/**
 * 根据能力类型查找Agent
 */
export function findAgentsByCapability(capabilityType: string): Promise<Agent[]> {
  return request.get('/agents/by-capability', { params: { capabilityType } })
}

/**
 * 获取仪表盘统计数据
 */
export function getDashboardStats(): Promise<{
  agentCount: number
  agentOnline: number
  agentOffline: number
  agentBusy: number
  agentDisabled: number
  taskCount: number
  taskPending: number
  taskRunning: number
  taskCompleted: number
  taskFailed: number
  taskCancelled: number
  successRate: number
  avgDuration: number
}> {
  return request.get('/dashboard/stats')
}