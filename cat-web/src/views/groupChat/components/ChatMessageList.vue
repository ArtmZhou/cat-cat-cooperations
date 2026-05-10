<template>
  <div class="chat-messages" ref="messagesRef">
    <div v-if="messages.length === 0" class="empty-chat">
      <div class="empty-icon">💬</div>
      <p>开始群聊吧！</p>
      <p class="hint">发送消息给所有Agent，或在输入框中输入 @ 指定Agent</p>
    </div>

    <div
      v-for="(msg, index) in messages"
      :key="msg.id || index"
      :class="['message', msg.senderType]"
    >
      <div class="message-avatar">
        {{ getMessageAvatar(msg) }}
      </div>
      <div class="message-content">
        <div class="message-header">
          <span class="sender" :class="'sender-' + msg.senderType">
            {{ msg.senderName || '未知' }}
          </span>
          <span v-if="msg.broadcast" class="broadcast-tag">📢 广播</span>
          <span v-if="msg.mentionedAgentIds?.length" class="mention-tag">
            @{{ getMentionNames(msg.mentionedAgentIds) }}
          </span>
          <span class="time">{{ formatTime(msg.createdAt) }}</span>
        </div>
        <div class="message-text" v-html="formatMessage(msg.content)"></div>
      </div>
    </div>

    <!-- 实时输出指示器 -->
    <div v-for="(output, agentId) in activeOutputs" :key="'output-' + agentId" class="message agent streaming">
      <div class="message-avatar">🤖</div>
      <div class="message-content">
        <div class="message-header">
          <span class="sender sender-agent">{{ getAgentName(agentId) }}</span>
          <span class="streaming-indicator">
            <span class="spinner-icon">{{ spinnerFrame }}</span> 输出中...
          </span>
        </div>
        <div class="message-text" v-html="formatMessage(output)"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { ChatMessage, AgentBrief } from '@/types/models'

const props = defineProps<{
  messages: ChatMessage[]
  activeOutputs: Record<string, string>
  spinnerFrame: string
  agents: AgentBrief[]
}>()

const messagesRef = ref<HTMLElement | null>(null)

function getMessageAvatar(msg: ChatMessage): string {
  if (msg.senderType === 'user') return '👤'
  if (msg.senderType === 'system') return 'ℹ️'
  return '🤖'
}

function getAgentName(agentId: string): string {
  const agent = props.agents.find(a => a.id === agentId)
  return agent?.name || agentId
}

function getMentionNames(agentIds: string[]): string {
  return agentIds.map(id => getAgentName(id)).join(', ')
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return date.toLocaleTimeString()
  } catch {
    return dateStr
  }
}

function formatMessage(content: string): string {
  if (!content) return '<span class="empty-content">...</span>'
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  return escaped
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

watch(
  () => props.messages,
  () => nextTick(() => scrollToBottom()),
  { deep: true }
)

watch(
  () => props.activeOutputs,
  () => nextTick(() => scrollToBottom()),
  { deep: true }
)
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: $bg-base;
}

.empty-chat {
  text-align: center;
  padding: 60px 20px;
  color: $text-muted;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    color: $text-secondary;
  }

  .hint {
    font-size: 12px;
    color: $text-muted;
  }
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  background: $bg-surface;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  border: 1px solid $border-subtle;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  border-color: transparent;
}

.message.system .message-avatar {
  background: $bg-hover;
  border-color: $border-subtle;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.sender {
  font-weight: 600;
  font-size: 13px;
}

.sender-user {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sender-agent { color: $status-running; }
.sender-system { color: $text-muted; }

.broadcast-tag {
  font-size: 11px;
  color: $warning;
}

.mention-tag {
  font-size: 11px;
  color: $color-violet;
}

.time {
  font-size: 11px;
  color: $text-muted;
}

.message-text {
  background: $bg-surface;
  padding: 10px 14px;
  border-radius: $radius-md;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid $border-subtle;
  color: $text-primary;
  word-break: break-word;

  :deep(code) {
    background: $bg-hover;
    padding: 1px 5px;
    border-radius: 4px;
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 13px;
  }
}

.message.user .message-text {
  background: rgba(124, 58, 237, 0.08);
  border-color: rgba(124, 58, 237, 0.15);
}

.message.system .message-text {
  background: $bg-hover;
  border-color: $border-subtle;
  font-size: 13px;
  color: $text-secondary;
}

.message.agent .message-text {
  background: $bg-surface;
  border-color: $border-subtle;
  border-left: 3px solid $color-violet;
}

.message.streaming .message-text {
  border-color: rgba(124, 58, 237, 0.3);
  animation: breathe 2s infinite;
}

.streaming-indicator {
  font-size: 12px;
  color: $color-violet;
  display: flex;
  align-items: center;
  gap: 4px;
}

.spinner-icon {
  font-weight: bold;
  color: $color-violet;
}

.empty-content {
  color: $text-muted;
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
