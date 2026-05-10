<template>
  <div class="group-chat">
    <ChatSidebar
      :groups="groups"
      :selected-id="selectedGroup?.id"
      @select="selectGroup"
      @create="showCreateDialog = true"
      @action="handleGroupAction"
    />

    <div class="chat-main">
      <template v-if="selectedGroup">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="header-info">
            <span class="group-avatar-large">👥</span>
            <div class="header-text">
              <h2>{{ selectedGroup.name }}</h2>
              <p>{{ selectedGroup.description || '多Agent群聊' }}</p>
            </div>
          </div>
          <div class="header-actions">
            <div class="agent-tags">
              <el-tag
                v-for="agent in selectedGroup.agents"
                :key="agent.id"
                size="small"
                :type="getAgentTagType(agent.status)"
                class="agent-tag"
              >
                🤖 {{ agent.name }}
              </el-tag>
            </div>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>

        <ChatMessageList
          :messages="messages"
          :active-outputs="activeOutputs"
          :spinner-frame="spinnerFrame"
          :agents="selectedGroup.agents"
        />

        <ChatInput
          :agents="selectedGroup.agents"
          :mentioned-agent-ids="mentionedAgentIds"
          :is-sending="isSending"
          @send="handleSend"
          @toggle-mention="toggleMention"
          @remove-mention="removeMention"
          @clear-mentions="mentionedAgentIds = []"
          @select-mention-from-input="handleSelectMentionFromInput"
        />
      </template>

      <div v-else class="no-group-selected">
        <div class="empty-icon">👈</div>
        <h2>选择一个群组开始聊天</h2>
        <p>从左侧选择群组，或创建一个新群组</p>
      </div>
    </div>

    <GroupCreateDialog
      v-model:visible="showCreateDialog"
      :editing="!!editingGroup"
      :saving="savingGroup"
      :form="groupForm"
      :all-agents="allAgents"
      @save="handleSaveGroup"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listChatGroups,
  createChatGroup,
  updateChatGroup,
  deleteChatGroup,
  sendGroupMessage,
  getGroupMessages,
  clearGroupMessages
} from '@/api/chatGroup'
import { getAgents } from '@/api/cliAgent'
import { cliWebSocket } from '@/utils/websocket'
import type { ChatGroup, ChatMessage, GroupForm } from '@/types/models'
import ChatSidebar from './components/ChatSidebar.vue'
import ChatMessageList from './components/ChatMessageList.vue'
import ChatInput from './components/ChatInput.vue'
import GroupCreateDialog from './components/GroupCreateDialog.vue'

// ===== State =====
const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const messages = ref<ChatMessage[]>([])
const allAgents = ref<Array<{ id: string; name: string; status: string }>>([])
const wsConnected = ref(false)

// Group form / dialog
const showCreateDialog = ref(false)
const editingGroup = ref<ChatGroup | null>(null)
const savingGroup = ref(false)
const groupForm = ref<GroupForm>({
  name: '',
  description: '',
  agentIds: []
})

// Message sending
const isSending = ref(false)
const mentionedAgentIds = ref<string[]>([])

// Streaming output
const activeOutputs = ref<Record<string, string>>({})

// Spinner
const spinnerFrame = ref('⠋')
const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
let spinnerInterval: ReturnType<typeof setInterval> | null = null

// ===== Lifecycle =====
onMounted(async () => {
  await Promise.all([loadGroups(), loadAllAgents()])

  try {
    await cliWebSocket.connect()
    wsConnected.value = true

    if (selectedGroup.value) {
      subscribeToGroup(selectedGroup.value.id)
    }
  } catch (error) {
    console.error('WebSocket connection failed:', error)
  }
})

onUnmounted(() => {
  stopSpinner()
  if (selectedGroup.value) {
    cliWebSocket.unsubscribeGroup(selectedGroup.value.id)
  }
})

// ===== Watch =====
watch(selectedGroup, (newGroup, oldGroup) => {
  if (oldGroup) {
    cliWebSocket.unsubscribeGroup(oldGroup.id)
  }

  if (newGroup && wsConnected.value) {
    subscribeToGroup(newGroup.id)
  }

  // Reset state on group switch
  activeOutputs.value = {}
  mentionedAgentIds.value = []
})

// ===== Data Loading =====
async function loadGroups() {
  try {
    groups.value = await listChatGroups() || []
  } catch (error) {
    console.error('加载群组列表失败:', error)
  }
}

async function loadAllAgents() {
  try {
    const result = await getAgents({ page: 1, pageSize: 100 })
    allAgents.value = (result.items || []).map((a: any) => ({
      id: a.id,
      name: a.name,
      status: a.status
    }))
  } catch (error) {
    console.error('加载Agent列表失败:', error)
  }
}

async function loadGroupMessages(groupId: string) {
  try {
    messages.value = await getGroupMessages(groupId) || []
  } catch (error) {
    console.error('加载群聊消息失败:', error)
    messages.value = []
  }
}

// ===== Group Selection =====
async function selectGroup(group: ChatGroup) {
  selectedGroup.value = group
  await loadGroupMessages(group.id)
}

// ===== WebSocket =====
function subscribeToGroup(groupId: string) {
  cliWebSocket.unsubscribeGroup(groupId)

  cliWebSocket.subscribeGroupMessage(groupId, (data: ChatMessage) => {
    if (!messages.value.find(m => m.id === data.id)) {
      messages.value.push(data)
    }
  })

  cliWebSocket.subscribeGroupAgentOutput(groupId, (data: any) => {
    const { agentId, type, content } = data

    if (type === 'text_delta' && content) {
      if (!activeOutputs.value[agentId]) {
        activeOutputs.value[agentId] = ''
        startSpinner()
      }
      activeOutputs.value[agentId] += content
    } else if (type === 'done') {
      delete activeOutputs.value[agentId]

      if (Object.keys(activeOutputs.value).length === 0) {
        stopSpinner()
      }

      if (selectedGroup.value) {
        loadGroupMessages(selectedGroup.value.id)
      }
    } else if (type === 'error') {
      delete activeOutputs.value[agentId]
      if (Object.keys(activeOutputs.value).length === 0) {
        stopSpinner()
      }
      if (selectedGroup.value) {
        loadGroupMessages(selectedGroup.value.id)
      }
    }
  })
}

// ===== Message Sending =====
async function handleSend(content: string) {
  if (!selectedGroup.value) return

  isSending.value = true

  try {
    await sendGroupMessage(selectedGroup.value.id, {
      content,
      mentionedAgentIds: mentionedAgentIds.value.length > 0 ? [...mentionedAgentIds.value] : null
    })

    mentionedAgentIds.value = []
    startSpinner()
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    isSending.value = false
  }
}

// ===== @Mention Handlers =====
function toggleMention(agentId: string) {
  const index = mentionedAgentIds.value.indexOf(agentId)
  if (index >= 0) {
    mentionedAgentIds.value.splice(index, 1)
  } else {
    mentionedAgentIds.value.push(agentId)
  }
}

function removeMention(agentId: string) {
  const index = mentionedAgentIds.value.indexOf(agentId)
  if (index >= 0) {
    mentionedAgentIds.value.splice(index, 1)
  }
}

function handleSelectMentionFromInput(agentId: string) {
  if (!mentionedAgentIds.value.includes(agentId)) {
    mentionedAgentIds.value.push(agentId)
  }
}

// ===== Group CRUD =====
async function handleSaveGroup() {
  if (!groupForm.value.name.trim()) {
    ElMessage.warning('请输入群组名称')
    return
  }
  if (groupForm.value.agentIds.length === 0) {
    ElMessage.warning('请至少选择一个Agent')
    return
  }

  savingGroup.value = true
  try {
    if (editingGroup.value) {
      const updated = await updateChatGroup(editingGroup.value.id, groupForm.value)
      ElMessage.success('群组已更新')
      if (selectedGroup.value?.id === editingGroup.value.id && updated) {
        selectedGroup.value = updated as ChatGroup
      }
    } else {
      await createChatGroup(groupForm.value)
      ElMessage.success('群组已创建')
    }

    showCreateDialog.value = false
    editingGroup.value = null
    groupForm.value = { name: '', description: '', agentIds: [] }
    await loadGroups()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    savingGroup.value = false
  }
}

function handleGroupAction(command: string, group: ChatGroup) {
  if (command === 'edit') {
    editingGroup.value = group
    groupForm.value = {
      name: group.name,
      description: group.description || '',
      agentIds: [...group.agentIds]
    }
    showCreateDialog.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('确定要删除该群组吗？', '确认删除', {
      type: 'warning'
    }).then(async () => {
      try {
        await deleteChatGroup(group.id)
        ElMessage.success('群组已删除')
        if (selectedGroup.value?.id === group.id) {
          selectedGroup.value = null
          messages.value = []
        }
        await loadGroups()
      } catch (error: any) {
        ElMessage.error(error.message || '删除失败')
      }
    }).catch(() => {})
  }
}

async function clearChat() {
  if (!selectedGroup.value) return

  try {
    await clearGroupMessages(selectedGroup.value.id)
    messages.value = []
    ElMessage.success('对话已清空')
  } catch (error: any) {
    ElMessage.error(error.message || '清空失败')
  }
}

// ===== Helpers =====
function startSpinner() {
  if (spinnerInterval) return
  let i = 0
  spinnerInterval = setInterval(() => {
    spinnerFrame.value = spinnerFrames[i % spinnerFrames.length]
    i++
  }, 80)
}

function stopSpinner() {
  if (spinnerInterval) {
    clearInterval(spinnerInterval)
    spinnerInterval = null
  }
}

function getAgentTagType(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'success',
    EXECUTING: 'primary',
    STOPPED: 'info',
    ERROR: 'danger'
  }
  return map[status] || 'info'
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.group-chat {
  display: flex;
  height: calc(100vh - 64px - 48px);
  background: $bg-surface;
  border-radius: $radius-lg;
  overflow: hidden;
  border: 1px solid $border-subtle;
}

// ===== Right chat area =====
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: $bg-base;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: $bg-surface;
  flex-wrap: wrap;
  gap: 8px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-avatar-large {
  font-size: 28px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-text h2 {
  margin: 0;
  font-size: 16px;
  color: $text-primary;
}

.header-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: $text-muted;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.agent-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.agent-tag {
  font-size: 11px;
}

// ===== No Group Selected =====
.no-group-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $text-muted;
  background: $bg-base;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 20px;
    opacity: 0.4;
  }

  h2 {
    margin: 0 0 8px;
    color: $text-primary;
  }
}
</style>
