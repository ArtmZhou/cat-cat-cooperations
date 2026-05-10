import { ref, onUnmounted } from 'vue'
import { cliWebSocket } from '@/utils/websocket'
import type { ChatMessage } from '@/types/models'

export function useChatWebSocket(
  groupId: string,
  onMessage: (msg: ChatMessage) => void,
  onAgentOutput: (data: any) => void
) {
  const wsConnected = ref(false)

  async function connect() {
    try {
      await cliWebSocket.connect()
      wsConnected.value = true
      subscribe()
    } catch (error) {
      console.error('WebSocket connection failed:', error)
    }
  }

  function subscribe() {
    cliWebSocket.unsubscribeGroup(groupId)
    cliWebSocket.subscribeGroupMessage(groupId, (data: ChatMessage) => onMessage(data))
    cliWebSocket.subscribeGroupAgentOutput(groupId, (data: any) => onAgentOutput(data))
  }

  function disconnect() {
    cliWebSocket.unsubscribeGroup(groupId)
    wsConnected.value = false
  }

  onUnmounted(disconnect)

  return { wsConnected, connect, disconnect, subscribe }
}
