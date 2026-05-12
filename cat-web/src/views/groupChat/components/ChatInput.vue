<template>
  <div class="chat-input">
    <!-- @提及选择器 -->
    <MentionPopup
      v-if="showMentionPopup"
      :agents="mentionPopupAgents"
      :mentioned-agent-ids="mentionedAgentIds"
      :highlight-index="mentionHighlightIndex"
      :mode="mentionAtPosition >= 0 ? 'filter' : 'select'"
      @toggle="(id: string) => $emit('toggleMention', id)"
      @select-from-filter="selectMentionFromInput"
    />

    <!-- 已@的Agent标签 -->
    <div v-if="mentionedAgentIds.length > 0" class="mention-tags">
      <el-tag
        v-for="agentId in mentionedAgentIds"
        :key="agentId"
        closable
        size="small"
        type="primary"
        @close="$emit('removeMention', agentId)"
      >
        @{{ getAgentName(agentId) }}
      </el-tag>
      <el-button text size="small" @click="$emit('clearMentions')">清除全部</el-button>
    </div>

    <div class="input-row">
      <el-button
        :type="showMentionPopup ? 'primary' : 'default'"
        size="small"
        @click="toggleMentionPopupButton"
        class="mention-btn"
      >
        @
      </el-button>
      <el-input
        ref="inputRef"
        v-model="inputText"
        type="textarea"
        :rows="3"
        :placeholder="inputPlaceholder"
        @keydown="handleInputKeydown"
      />
    </div>
    <div class="input-actions">
      <div class="input-hints">
        <span v-if="mentionedAgentIds.length > 0">
          📌 消息将发送给 {{ mentionedAgentIds.length }} 个Agent
        </span>
        <span v-else>📢 消息将广播给所有Agent · 按 Enter 发送</span>
      </div>
      <el-button
        type="primary"
        @click="handleSend"
        :loading="isSending"
        :disabled="!inputText.trim()"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import type { AgentBrief } from '@/types/models'
import MentionPopup from './MentionPopup.vue'

const props = defineProps<{
  agents: AgentBrief[]
  mentionedAgentIds: string[]
  isSending: boolean
}>()

const emit = defineEmits<{
  send: [content: string]
  toggleMention: [agentId: string]
  removeMention: [agentId: string]
  clearMentions: []
  selectMentionFromInput: [agentId: string]
}>()

// Internal state
const inputText = ref('')
const inputRef = ref<any>(null)
const showMentionPopup = ref(false)
const mentionFilter = ref('')
const mentionAtPosition = ref(-1)
const mentionHighlightIndex = ref(0)

// Computed
const inputPlaceholder = computed(() => {
  if (props.mentionedAgentIds.length > 0) {
    return '输入消息发送给指定Agent... (Enter发送)'
  }
  return '输入消息广播给所有Agent... (Enter发送，输入@指定Agent)'
})

const filteredMentionAgents = computed(() => {
  const filter = mentionFilter.value.toLowerCase()
  if (!filter) return props.agents
  return props.agents.filter(a => a.name.toLowerCase().includes(filter))
})

const mentionPopupAgents = computed(() => {
  if (mentionAtPosition.value >= 0) {
    return filteredMentionAgents.value
  }
  return props.agents
})

// Methods
function getAgentName(agentId: string): string {
  const agent = props.agents.find(a => a.id === agentId)
  return agent?.name || agentId
}

function handleSend() {
  if (!inputText.value.trim()) return
  const content = inputText.value.trim()
  inputText.value = ''
  showMentionPopup.value = false
  emit('send', content)
}

function toggleMention(agentId: string) {
  emit('toggleMention', agentId)
}

function selectMentionFromInput(agentId: string) {
  emit('selectMentionFromInput', agentId)
  // Remove @query text from input
  const atPos = mentionAtPosition.value
  if (atPos >= 0) {
    const before = inputText.value.substring(0, atPos)
    const afterCursor = inputText.value.substring(atPos + 1 + mentionFilter.value.length)
    inputText.value = before + afterCursor
  }
  closeMentionPopupFromInput()
}

function handleInputChange() {
  const textareaEl = inputRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
  if (!textareaEl) return

  const value = inputText.value
  const cursorPos = textareaEl.selectionStart || value.length
  const textBeforeCursor = value.substring(0, cursorPos)
  const lastAtIndex = textBeforeCursor.lastIndexOf('@')

  if (lastAtIndex >= 0) {
    const charBefore = lastAtIndex > 0 ? textBeforeCursor[lastAtIndex - 1] : ' '
    if (charBefore === ' ' || charBefore === '\n' || lastAtIndex === 0) {
      const query = textBeforeCursor.substring(lastAtIndex + 1)
      if (!query.includes(' ') && !query.includes('\n')) {
        mentionFilter.value = query
        mentionAtPosition.value = lastAtIndex
        mentionHighlightIndex.value = 0
        showMentionPopup.value = true
        return
      }
    }
  }

  if (mentionAtPosition.value >= 0) {
    closeMentionPopupFromInput()
  }
}

function handleInputKeydown(event: KeyboardEvent) {
  if (showMentionPopup.value && mentionAtPosition.value >= 0) {
    const agents = filteredMentionAgents.value
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      mentionHighlightIndex.value = (mentionHighlightIndex.value + 1) % agents.length
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      mentionHighlightIndex.value = (mentionHighlightIndex.value - 1 + agents.length) % agents.length
    } else if (event.key === 'Enter' && agents.length > 0) {
      event.preventDefault()
      selectMentionFromInput(agents[mentionHighlightIndex.value].id)
    } else if (event.key === 'Escape') {
      event.preventDefault()
      closeMentionPopupFromInput()
    }
    return
  }

  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleSend()
  }
}

function closeMentionPopupFromInput() {
  mentionFilter.value = ''
  mentionAtPosition.value = -1
  mentionHighlightIndex.value = 0
  showMentionPopup.value = false
}

function toggleMentionPopupButton() {
  if (mentionAtPosition.value >= 0) {
    closeMentionPopupFromInput()
    return
  }
  showMentionPopup.value = !showMentionPopup.value
}

// Watch input text for @ detection
watch(inputText, () => {
  nextTick(() => handleInputChange())
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-input {
  padding: 12px 20px;
  border-top: 1px solid $border-subtle;
  background: $bg-surface;
  position: relative;
}

.mention-tags {
  padding: 4px 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  border-bottom: 1px solid $border-subtle;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.mention-btn {
  font-weight: bold;
  font-size: 16px;
  min-width: 36px;
  flex-shrink: 0;
  margin-top: 4px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hints {
  font-size: 12px;
  color: $text-muted;
}
</style>
