// Workspace API - Git Worktree 工作空间管理
import request from '@/utils/request'

// ===== Types =====

export type WorkspaceStatus = 'ACTIVE' | 'COMMITTED' | 'MERGED' | 'REMOVED' | 'ERROR'

export interface WorkspaceInfo {
  id: string
  projectPath: string
  worktreePath: string
  branchName: string
  baseBranch: string
  status: WorkspaceStatus
  taskId?: string
  agentId?: string
  description?: string
  lastCommitHash?: string
  lastCommitMessage?: string
  createdAt: string
  updatedAt: string
}

export interface CreateWorkspaceRequest {
  projectPath: string
  branchName?: string
  baseBranch?: string
  taskId?: string
  agentId?: string
  description?: string
}

export interface WorkspaceGitStatus {
  workspaceId: string
  branchName: string
  currentCommit: string
  modifiedFiles: number
  stagedFiles: number
  untrackedFiles: number
  hasUncommittedChanges: boolean
  aheadCount: number
  behindCount: number
  modifiedFileList: string[]
  untrackedFileList: string[]
}

export interface CommitResult {
  success: boolean
  commitHash?: string
  message?: string
  error?: string
}

export interface PushResult {
  success: boolean
  branchName?: string
  remoteName?: string
  error?: string
}

export interface MergeResult {
  success: boolean
  mergeCommitHash?: string
  sourceBranch?: string
  targetBranch?: string
  hasConflicts: boolean
  conflictFiles: string[]
  error?: string
}

export interface ConflictCheckResult {
  hasConflicts: boolean
  conflictFiles: string[]
  totalChangedFiles: number
  error?: string
}

export interface SyncResult {
  success: boolean
  strategy: string
  hasConflicts: boolean
  conflictFiles: string[]
  error?: string
}

// ===== API Functions =====

/**
 * 创建工作空间
 */
export function createWorkspace(data: CreateWorkspaceRequest) {
  return request.post('/workspaces', data)
}

/**
 * 获取工作空间详情
 */
export function getWorkspace(id: string) {
  return request.get(`/workspaces/${id}`)
}

/**
 * 列出所有工作空间
 */
export function listWorkspaces(projectPath?: string) {
  return request.get('/workspaces', { params: projectPath ? { projectPath } : {} })
}

/**
 * 删除工作空间
 */
export function removeWorkspace(id: string, force = false) {
  return request.delete(`/workspaces/${id}`, { params: { force } })
}

/**
 * 获取工作空间Git状态
 */
export function getWorkspaceGitStatus(id: string) {
  return request.get(`/workspaces/${id}/git-status`)
}

/**
 * 在工作空间中提交变更
 */
export function commitChanges(id: string, message: string) {
  return request.post(`/workspaces/${id}/commit`, { message })
}

/**
 * 推送工作空间分支到远端
 */
export function pushBranch(id: string) {
  return request.post(`/workspaces/${id}/push`)
}

/**
 * 合并工作空间分支到目标分支
 */
export function mergeBranch(id: string, targetBranch: string) {
  return request.post(`/workspaces/${id}/merge`, { targetBranch })
}

/**
 * 检测合并冲突
 */
export function checkConflicts(id: string, targetBranch: string) {
  return request.post(`/workspaces/${id}/check-conflicts`, { targetBranch })
}

/**
 * 同步主分支变更到工作空间
 */
export function syncFromBranch(id: string, sourceBranch: string) {
  return request.post(`/workspaces/${id}/sync`, { sourceBranch })
}

/**
 * 使用工作空间执行任务
 */
export function executeTaskWithWorkspace(agentId: string, data: {
  input: string
  timeoutSeconds?: number
  projectPath: string
  baseBranch?: string
  description?: string
}) {
  return request.post(`/cli-agents/${agentId}/tasks/execute-with-workspace`, data)
}
