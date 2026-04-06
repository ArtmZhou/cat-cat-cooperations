import { request } from '@/utils/request'

// ===== Types =====

export type TaskType = 'SIMPLE' | 'WORKFLOW' | 'PARALLEL' | 'NEGOTIATION'
export type TaskStatus = 'PENDING' | 'ASSIGNED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
export type TaskPriority = 0 | 1 | 2 // 0-低 1-中 2-高

export interface Task {
  id: string
  name: string
  description?: string
  type: TaskType
  status: TaskStatus
  priority: TaskPriority
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  config?: Record<string, unknown>
  timeoutSeconds: number
  retryCount: number
  maxRetry: number
  scheduledAt?: string
  startedAt?: string
  completedAt?: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TaskAssignment {
  id: string
  taskId: string
  agentId: string
  role: 'MAIN' | 'HELPER' | 'VOTER'
  status: 'ASSIGNED' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  assignedAt: string
  startedAt?: string
  completedAt?: string
  result?: Record<string, unknown>
  errorMessage?: string
}

export interface TaskLog {
  id: string
  taskId: string
  agentId?: string
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'
  message: string
  detail?: Record<string, unknown>
  createdAt: string
}

export interface CreateTaskRequest {
  name: string
  description?: string
  type: TaskType
  priority?: TaskPriority
  input?: Record<string, unknown>
  config?: Record<string, unknown>
  timeoutSeconds?: number
  maxRetry?: number
  scheduledAt?: string
  agentIds?: string[]
}

export interface TaskListParams {
  page?: number
  pageSize?: number
  status?: TaskStatus
  type?: TaskType
  priority?: TaskPriority
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
 * 获取任务列表
 */
export function getTaskList(params: TaskListParams): Promise<PageResult<Task>> {
  return request.get('/tasks', { params })
}

/**
 * 获取任务详情
 */
export function getTask(id: string): Promise<Task> {
  return request.get(`/tasks/${id}`)
}

/**
 * 创建任务
 */
export function createTask(data: CreateTaskRequest): Promise<Task> {
  return request.post('/tasks', data)
}

/**
 * 更新任务
 */
export function updateTask(id: string, data: Partial<CreateTaskRequest>): Promise<Task> {
  return request.put(`/tasks/${id}`, data)
}

/**
 * 删除任务
 */
export function deleteTask(id: string): Promise<void> {
  return request.delete(`/tasks/${id}`)
}

/**
 * 取消任务
 */
export function cancelTask(id: string): Promise<void> {
  return request.post(`/tasks/${id}/cancel`)
}

/**
 * 重试任务
 */
export function retryTask(id: string): Promise<Task> {
  return request.post(`/tasks/${id}/retry`)
}

/**
 * 获取任务分配列表
 */
export function getTaskAssignments(taskId: string): Promise<TaskAssignment[]> {
  return request.get(`/tasks/${taskId}/assignments`)
}

/**
 * 获取任务日志
 */
export function getTaskLogs(taskId: string, params?: { level?: string }): Promise<TaskLog[]> {
  return request.get(`/tasks/${taskId}/logs`, { params })
}

/**
 * 获取任务统计
 */
export function getTaskStatistics(): Promise<{
  total: number
  pending: number
  running: number
  completed: number
  failed: number
  successRate: number
}> {
  return request.get('/tasks/statistics')
}

/**
 * 定时任务：创建调度
 */
export function scheduleTask(taskId: string, data: {
  executeAt?: string
  cronExpression?: string
}): Promise<void> {
  return request.post(`/tasks/${taskId}/schedule`, data)
}

/**
 * 定时任务：取消调度
 */
export function cancelSchedule(taskId: string): Promise<void> {
  return request.delete(`/tasks/${taskId}/schedule`)
}

/**
 * Agent执行任务
 */
export function executeTask(taskId: string, agentId: string): Promise<TaskAssignment> {
  return request.post(`/tasks/${taskId}/execute`, { agentId })
}

/**
 * 提交任务结果
 */
export function submitTaskResult(taskId: string, agentId: string, data: {
  success: boolean
  output?: Record<string, unknown>
  errorMessage?: string
}): Promise<void> {
  return request.post(`/tasks/${taskId}/result`, { agentId, ...data })
}