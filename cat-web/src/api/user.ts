import { request } from '@/utils/request'

// ===== Types =====

export interface User {
  id: string
  username: string
  email?: string
  nickname?: string
  avatar?: string
  status: number // 1-启用 0-禁用
  createdAt: string
  updatedAt: string
  roles?: Role[]
}

export interface Role {
  id: string
  name: string
  code: string
  description?: string
  isSystem: number
  createdAt: string
  permissions?: Permission[]
}

export interface Permission {
  id: string
  name: string
  code: string
  resourceType: string
  resourcePath: string
  parentId?: string
}

export interface CreateUserRequest {
  username: string
  password: string
  email?: string
  nickname?: string
  roleIds?: string[]
}

export interface UpdateUserRequest {
  email?: string
  nickname?: string
  avatar?: string
  status?: number
  roleIds?: string[]
}

export interface UserListParams {
  page?: number
  pageSize?: number
  status?: number
  username?: string
}

export interface CreateRoleRequest {
  name: string
  code: string
  description?: string
  permissionIds?: string[]
}

export interface UpdateRoleRequest {
  name?: string
  description?: string
  permissionIds?: string[]
}

export interface PageResult<T> {
  items: T[]
  total: number
  pageSize: number
  page: number
  totalPages: number
}

// ===== 用户管理 API =====

/**
 * 获取用户列表
 */
export function getUserList(params: UserListParams): Promise<PageResult<User>> {
  return request.get('/users', { params })
}

/**
 * 获取用户详情
 */
export function getUser(id: string): Promise<User> {
  return request.get(`/users/${id}`)
}

/**
 * 创建用户
 */
export function createUser(data: CreateUserRequest): Promise<User> {
  return request.post('/users', data)
}

/**
 * 更新用户
 */
export function updateUser(id: string, data: UpdateUserRequest): Promise<User> {
  return request.put(`/users/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id: string): Promise<void> {
  return request.delete(`/users/${id}`)
}

/**
 * 重置用户密码
 */
export function resetUserPassword(id: string, newPassword: string): Promise<void> {
  return request.put(`/users/${id}/password`, { newPassword })
}

/**
 * 启用用户
 */
export function enableUser(id: string): Promise<void> {
  return request.put(`/users/${id}/enable`)
}

/**
 * 禁用用户
 */
export function disableUser(id: string): Promise<void> {
  return request.put(`/users/${id}/disable`)
}

/**
 * 获取用户角色
 */
export function getUserRoles(userId: string): Promise<Role[]> {
  return request.get(`/users/${userId}/roles`)
}

/**
 * 分配用户角色
 */
export function assignUserRoles(userId: string, roleIds: string[]): Promise<void> {
  return request.put(`/users/${userId}/roles`, { roleIds })
}

// ===== 角色管理 API =====

/**
 * 获取角色列表
 */
export function getRoleList(): Promise<Role[]> {
  return request.get('/roles')
}

/**
 * 获取角色详情
 */
export function getRole(id: string): Promise<Role> {
  return request.get(`/roles/${id}`)
}

/**
 * 创建角色
 */
export function createRole(data: CreateRoleRequest): Promise<Role> {
  return request.post('/roles', data)
}

/**
 * 更新角色
 */
export function updateRole(id: string, data: UpdateRoleRequest): Promise<Role> {
  return request.put(`/roles/${id}`, data)
}

/**
 * 删除角色
 */
export function deleteRole(id: string): Promise<void> {
  return request.delete(`/roles/${id}`)
}

/**
 * 获取角色权限
 */
export function getRolePermissions(roleId: string): Promise<Permission[]> {
  return request.get(`/roles/${roleId}/permissions`)
}

/**
 * 分配角色权限
 */
export function assignRolePermissions(roleId: string, permissionIds: string[]): Promise<void> {
  return request.put(`/roles/${roleId}/permissions`, { permissionIds })
}

// ===== 权限管理 API =====

/**
 * 获取所有权限
 */
export function getAllPermissions(): Promise<Permission[]> {
  return request.get('/permissions')
}

/**
 * 获取权限树
 */
export function getPermissionTree(): Promise<Permission[]> {
  return request.get('/permissions/tree')
}