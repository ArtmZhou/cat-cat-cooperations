// WebSocket服务 - 用于接收CLI Agent输出
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class CliWebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = new Map()
    this.connected = false
  }

  // 连接WebSocket
  connect() {
    if (this.client && this.connected) {
      return Promise.resolve()
    }

    return new Promise((resolve, reject) => {
      // 使用相对路径，通过 Vite 代理连接后端 WebSocket
      const wsUrl = `${window.location.protocol}//${window.location.host}/ws`
      this.client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log('WebSocket connected')
          this.connected = true
          resolve()
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected')
          this.connected = false
        },
        onStompError: (frame) => {
          console.error('STOMP error:', frame)
          reject(frame)
        }
      })

      this.client.activate()
    })
  }

  // 断开连接
  disconnect() {
    if (this.client) {
      // 取消所有订阅
      this.subscriptions.forEach(sub => sub.unsubscribe())
      this.subscriptions.clear()

      this.client.deactivate()
      this.client = null
      this.connected = false
    }
  }

  // 订阅Agent输出
  subscribeOutput(agentId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/cli/${agentId}/output`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ type: 'output', content: message.body })
      }
    })

    this.subscriptions.set(`output-${agentId}`, subscription)
    return subscription
  }

  // 订阅Agent错误
  subscribeError(agentId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/cli/${agentId}/error`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ type: 'error', content: message.body })
      }
    })

    this.subscriptions.set(`error-${agentId}`, subscription)
    return subscription
  }

  // 订阅状态变化
  subscribeStatus(agentId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/cli/status/${agentId}`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ agentId, status: message.body })
      }
    })

    this.subscriptions.set(`status-${agentId}`, subscription)
    return subscription
  }

  // 取消订阅
  unsubscribe(agentId) {
    const keys = [`output-${agentId}`, `error-${agentId}`, `status-${agentId}`]
    keys.forEach(key => {
      const sub = this.subscriptions.get(key)
      if (sub) {
        sub.unsubscribe()
        this.subscriptions.delete(key)
      }
    })
  }

  // 订阅群聊消息（新消息推送）
  subscribeGroupMessage(groupId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/chat-group/${groupId}/message`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ type: 'message', content: message.body })
      }
    })

    this.subscriptions.set(`group-msg-${groupId}`, subscription)
    return subscription
  }

  // 订阅群聊Agent流式输出
  subscribeGroupAgentOutput(groupId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/chat-group/${groupId}/agent-output`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ type: 'output', content: message.body })
      }
    })

    this.subscriptions.set(`group-agent-output-${groupId}`, subscription)
    return subscription
  }

  // 取消群聊订阅
  unsubscribeGroup(groupId) {
    const keys = [`group-msg-${groupId}`, `group-agent-output-${groupId}`, `group-auto-discussion-${groupId}`]
    keys.forEach(key => {
      const sub = this.subscriptions.get(key)
      if (sub) {
        sub.unsubscribe()
        this.subscriptions.delete(key)
      }
    })
  }

  // 订阅群聊自动讨论状态变更
  subscribeGroupAutoDiscussionStatus(groupId, callback) {
    if (!this.client || !this.connected) {
      console.warn('WebSocket not connected')
      return null
    }

    const topic = `/topic/chat-group/${groupId}/auto-discussion-status`
    const subscription = this.client.subscribe(topic, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        callback({ running: false })
      }
    })

    this.subscriptions.set(`group-auto-discussion-${groupId}`, subscription)
    return subscription
  }
}

// 单例模式
export const cliWebSocket = new CliWebSocketService()