import { request } from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function getCurrentUser(): Promise<UserInfo> {
  return request.get('/auth/me')
}