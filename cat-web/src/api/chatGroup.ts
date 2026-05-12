// 聊天群组 API
import request from '@/utils/request'
import type { GroupForm, GroupMessagePayload } from '@/types/models'

/** 创建聊天群组 */
export function createChatGroup(data: GroupForm): Promise<any> {
  return request.post('/chat-groups', data)
}

/** 更新聊天群组 */
export function updateChatGroup(groupId: string, data: Partial<GroupForm>): Promise<any> {
  return request.put(`/chat-groups/${groupId}`, data)
}

/** 删除聊天群组 */
export function deleteChatGroup(groupId: string): Promise<void> {
  return request.delete(`/chat-groups/${groupId}`)
}

/** 获取群组详情 */
export function getChatGroup(groupId: string): Promise<any> {
  return request.get(`/chat-groups/${groupId}`)
}

/** 获取所有群组列表 */
export function listChatGroups(): Promise<any[]> {
  return request.get('/chat-groups')
}

/** 发送群聊消息 */
export function sendGroupMessage(groupId: string, data: GroupMessagePayload): Promise<any> {
  return request.post(`/chat-groups/${groupId}/messages`, data)
}

/** 获取群聊历史消息 */
export function getGroupMessages(groupId: string, limit?: number): Promise<any[]> {
  return request.get(`/chat-groups/${groupId}/messages`, { params: { limit: limit ?? 100 } })
}

/** 清空群聊消息 */
export function clearGroupMessages(groupId: string): Promise<void> {
  return request.post(`/chat-groups/${groupId}/messages/clear`)
}
