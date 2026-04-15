// 聊天群组 API
import request from '@/utils/request'

/**
 * 创建聊天群组
 */
export function createChatGroup(data) {
  return request.post('/chat-groups', data)
}

/**
 * 更新聊天群组
 */
export function updateChatGroup(groupId, data) {
  return request.put(`/chat-groups/${groupId}`, data)
}

/**
 * 删除聊天群组
 */
export function deleteChatGroup(groupId) {
  return request.delete(`/chat-groups/${groupId}`)
}

/**
 * 获取群组详情
 */
export function getChatGroup(groupId) {
  return request.get(`/chat-groups/${groupId}`)
}

/**
 * 获取所有群组列表
 */
export function listChatGroups() {
  return request.get('/chat-groups')
}

/**
 * 发送群聊消息
 * @param {string} groupId - 群组ID
 * @param {object} data - { content: string, mentionedAgentIds?: string[] }
 */
export function sendGroupMessage(groupId, data) {
  return request.post(`/chat-groups/${groupId}/messages`, data)
}

/**
 * 获取群聊历史消息
 */
export function getGroupMessages(groupId, limit = 100) {
  return request.get(`/chat-groups/${groupId}/messages`, { params: { limit } })
}

/**
 * 清空群聊消息
 */
export function clearGroupMessages(groupId) {
  return request.post(`/chat-groups/${groupId}/messages/clear`)
}
