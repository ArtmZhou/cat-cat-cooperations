<template>
  <div class="group-chat">
    <!-- 左侧群组列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>群聊</h3>
        <el-button size="small" type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <div class="group-list">
        <div
          v-for="group in groups"
          :key="group.id"
          :class="['group-item', { active: selectedGroup?.id === group.id }]"
          @click="selectGroup(group)"
        >
          <div class="group-avatar">👥</div>
          <div class="group-info">
            <div class="group-name">{{ group.name }}</div>
            <div class="group-meta">{{ group.agents?.length || 0 }} 个Agent</div>
          </div>
          <el-dropdown @command="handleGroupAction($event, group)" trigger="click" @click.stop>
            <el-button text size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <el-empty v-if="groups.length === 0" description="暂无群组，请创建" :image-size="60" />
      </div>
    </div>

    <!-- 右侧聊天区域 -->
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

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-chat">
            <div class="empty-icon">💬</div>
            <p>开始群聊吧！</p>
            <p class="hint">发送消息给所有Agent，或使用 @ 指定Agent</p>
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

        <!-- 输入区域 -->
        <div class="chat-input">
          <!-- @提及选择器 -->
          <div v-if="showMentionPopup" class="mention-popup">
            <div class="mention-header">选择要@的Agent：</div>
            <div
              v-for="agent in selectedGroup.agents"
              :key="agent.id"
              :class="['mention-item', { selected: mentionedAgentIds.includes(agent.id) }]"
              @click="toggleMention(agent.id)"
            >
              <span class="mention-avatar">🤖</span>
              <span class="mention-name">{{ agent.name }}</span>
              <el-tag size="small" :type="getAgentTagType(agent.status)">
                {{ getAgentStatusText(agent.status) }}
              </el-tag>
              <el-icon v-if="mentionedAgentIds.includes(agent.id)" class="mention-check"><Check /></el-icon>
            </div>
          </div>

          <!-- 已@的Agent标签 -->
          <div v-if="mentionedAgentIds.length > 0" class="mention-tags">
            <el-tag
              v-for="agentId in mentionedAgentIds"
              :key="agentId"
              closable
              size="small"
              type="primary"
              @close="removeMention(agentId)"
            >
              @{{ getAgentName(agentId) }}
            </el-tag>
            <el-button text size="small" @click="mentionedAgentIds = []">清除全部</el-button>
          </div>

          <div class="input-row">
            <el-button
              :type="showMentionPopup ? 'primary' : 'default'"
              size="small"
              @click="showMentionPopup = !showMentionPopup"
              class="mention-btn"
            >
              @
            </el-button>
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              :placeholder="inputPlaceholder"
              @keydown.enter.exact.prevent="sendMessage"
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
              @click="sendMessage"
              :loading="isSending"
              :disabled="!inputText.trim()"
            >
              发送
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="no-group-selected">
        <div class="empty-icon">👈</div>
        <h2>选择一个群组开始聊天</h2>
        <p>从左侧选择群组，或创建一个新群组</p>
      </div>
    </div>

    <!-- 创建/编辑群组对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingGroup ? '编辑群组' : '创建群组'"
      width="500px"
    >
      <el-form :model="groupForm" label-width="80px">
        <el-form-item label="群组名称" required>
          <el-input v-model="groupForm.name" placeholder="输入群组名称" />
        </el-form-item>
        <el-form-item label="群组描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" placeholder="输入群组描述" />
        </el-form-item>
        <el-form-item label="选择Agent" required>
          <div class="agent-selector">
            <el-checkbox-group v-model="groupForm.agentIds">
              <div v-for="agent in allAgents" :key="agent.id" class="agent-checkbox-item">
                <el-checkbox :value="agent.id">
                  <span class="agent-checkbox-label">
                    🤖 {{ agent.name }}
                    <el-tag size="small" :type="getAgentTagType(agent.status)">
                      {{ getAgentStatusText(agent.status) }}
                    </el-tag>
                  </span>
                </el-checkbox>
              </div>
            </el-checkbox-group>
            <el-empty v-if="allAgents.length === 0" description="暂无Agent，请先创建" :image-size="40" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveGroup" :loading="savingGroup">
          {{ editingGroup ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled, Check } from '@element-plus/icons-vue'
import {
  listChatGroups,
  createChatGroup,
  updateChatGroup,
  deleteChatGroup,
  getChatGroup,
  sendGroupMessage,
  getGroupMessages,
  clearGroupMessages
} from '@/api/chatGroup'
import { getAgents } from '@/api/cliAgent'
import { cliWebSocket } from '@/utils/websocket'

// ===== Types =====
interface AgentBrief {
  id: string
  name: string
  status: string
}

interface ChatGroup {
  id: string
  name: string
  description: string
  agentIds: string[]
  agents: AgentBrief[]
  createdAt: string
  updatedAt: string
}

interface ChatMessage {
  id: string
  groupId: string
  senderType: string  // 'user' | 'agent' | 'system'
  senderAgentId?: string
  senderName: string
  content: string
  mentionedAgentIds: string[]
  broadcast: boolean
  createdAt: string
}

// ===== State =====
const groups = ref<ChatGroup[]>([])
const selectedGroup = ref<ChatGroup | null>(null)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isSending = ref(false)
const messagesRef = ref<HTMLElement | null>(null)
const wsConnected = ref(false)

// @mention state
const showMentionPopup = ref(false)
const mentionedAgentIds = ref<string[]>([])

// Streaming output state: agentId -> accumulated text
const activeOutputs = ref<Record<string, string>>({})

// All agents (for group creation)
const allAgents = ref<AgentBrief[]>([])

// Group form
const showCreateDialog = ref(false)
const editingGroup = ref<ChatGroup | null>(null)
const savingGroup = ref(false)
const groupForm = ref({
  name: '',
  description: '',
  agentIds: [] as string[]
})

// Spinner animation
const spinnerFrame = ref('⠋')
const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
let spinnerInterval: ReturnType<typeof setInterval> | null = null

// ===== Computed =====
const inputPlaceholder = computed(() => {
  if (mentionedAgentIds.value.length > 0) {
    return '输入消息发送给指定Agent... (Enter发送)'
  }
  return '输入消息广播给所有Agent... (Enter发送，点击@指定Agent)'
})

// ===== Lifecycle =====
onMounted(async () => {
  await Promise.all([loadGroups(), loadAllAgents()])

  // Connect WebSocket
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

// Watch group selection
watch(selectedGroup, (newGroup, oldGroup) => {
  if (oldGroup) {
    cliWebSocket.unsubscribeGroup(oldGroup.id)
  }

  if (newGroup && wsConnected.value) {
    subscribeToGroup(newGroup.id)
  }

  // Reset state
  activeOutputs.value = {}
  mentionedAgentIds.value = []
  showMentionPopup.value = false
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
    nextTick(() => scrollToBottom())
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

  // Subscribe to new messages
  cliWebSocket.subscribeGroupMessage(groupId, (data: ChatMessage) => {
    console.log('Group message received:', data)
    // Add message to list (avoid duplicates)
    if (!messages.value.find(m => m.id === data.id)) {
      messages.value.push(data)
      nextTick(() => scrollToBottom())
    }
  })

  // Subscribe to agent streaming output
  cliWebSocket.subscribeGroupAgentOutput(groupId, (data: any) => {
    console.log('Group agent output:', data)
    const { agentId, type, content } = data

    if (type === 'text_delta' && content) {
      // Accumulate streaming output
      if (!activeOutputs.value[agentId]) {
        activeOutputs.value[agentId] = ''
        startSpinner()
      }
      activeOutputs.value[agentId] += content
      nextTick(() => scrollToBottom())
    } else if (type === 'done') {
      // Agent finished - remove from active outputs
      // The final message is already saved server-side
      delete activeOutputs.value[agentId]

      if (Object.keys(activeOutputs.value).length === 0) {
        stopSpinner()
      }

      // Reload messages to get the final saved version
      if (selectedGroup.value) {
        loadGroupMessages(selectedGroup.value.id)
      }
    }
  })
}

// ===== Message Sending =====
async function sendMessage() {
  if (!inputText.value.trim() || !selectedGroup.value) return

  const content = inputText.value.trim()
  inputText.value = ''
  showMentionPopup.value = false
  isSending.value = true

  try {
    await sendGroupMessage(selectedGroup.value.id, {
      content,
      mentionedAgentIds: mentionedAgentIds.value.length > 0 ? [...mentionedAgentIds.value] : null
    })

    // Reset mentions after sending
    mentionedAgentIds.value = []
    startSpinner()
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    isSending.value = false
  }
}

// ===== @Mention =====
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
      await updateChatGroup(editingGroup.value.id, groupForm.value)
      ElMessage.success('群组已更新')
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

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function getMessageAvatar(msg: ChatMessage): string {
  if (msg.senderType === 'user') return '👤'
  if (msg.senderType === 'system') return 'ℹ️'
  return '🤖'
}

function getAgentName(agentId: string): string {
  const agent = selectedGroup.value?.agents?.find(a => a.id === agentId)
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
  return content
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
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

function getAgentStatusText(status: string): string {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    EXECUTING: '执行中',
    STOPPED: '已停止',
    ERROR: '错误'
  }
  return map[status] || status
}
</script>

<style scoped>
.group-chat {
  display: flex;
  height: calc(100vh - 64px - 48px);
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
}

/* 左侧群组列表 */
.chat-sidebar {
  width: 280px;
  border-right: 1px solid #F0E6D8;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #F0E6D8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
}

.group-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #F5F5F5;
  transition: background 0.2s;
}

.group-item:hover {
  background: #FFF9F5;
}

.group-item.active {
  background: #FFF5E6;
  border-left: 3px solid #FF8C42;
}

.group-avatar {
  width: 40px;
  height: 40px;
  background: #E6F7FF;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  font-size: 12px;
  color: #8C8C8C;
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #F0E6D8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #FAFAFA;
  flex-wrap: wrap;
  gap: 8px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-avatar-large {
  font-size: 32px;
}

.header-text h2 {
  margin: 0;
  font-size: 16px;
}

.header-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: #8C8C8C;
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

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #FAFAFA;
}

.empty-chat {
  text-align: center;
  padding: 60px 20px;
  color: #8C8C8C;
}

.empty-chat .empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-chat .hint {
  font-size: 12px;
  color: #BFBFBF;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  background: #FFF;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  border: 1px solid #F0E6D8;
}

.message.user .message-avatar {
  background: #FF8C42;
  border-color: #FF8C42;
}

.message.system .message-avatar {
  background: #F0F0F0;
  border-color: #E0E0E0;
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

.sender-user { color: #FF8C42; }
.sender-agent { color: #1890FF; }
.sender-system { color: #8C8C8C; }

.broadcast-tag {
  font-size: 11px;
  color: #FAAD14;
}

.mention-tag {
  font-size: 11px;
  color: #1890FF;
}

.time {
  font-size: 11px;
  color: #BFBFBF;
}

.message-text {
  background: white;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid #F0E6D8;
  word-break: break-word;
}

.message.user .message-text {
  background: #FFF5E6;
  border-color: #FFE8CC;
}

.message.system .message-text {
  background: #F7F7F7;
  border-color: #E8E8E8;
  font-size: 13px;
  color: #666;
}

.message.agent .message-text {
  background: #F0F9FF;
  border-color: #BAE7FF;
}

.message.streaming .message-text {
  border-color: #91D5FF;
  animation: pulse 2s infinite;
}

.streaming-indicator {
  font-size: 12px;
  color: #1890FF;
  display: flex;
  align-items: center;
  gap: 4px;
}

.spinner-icon {
  font-weight: bold;
}

.empty-content {
  color: #BFBFBF;
}

/* @Mention弹窗 */
.mention-popup {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #F0E6D8;
  border-radius: 8px;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.mention-header {
  padding: 8px 12px;
  font-size: 12px;
  color: #8C8C8C;
  border-bottom: 1px solid #F5F5F5;
}

.mention-item {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.mention-item:hover {
  background: #FFF9F5;
}

.mention-item.selected {
  background: #E6F7FF;
}

.mention-avatar {
  font-size: 16px;
}

.mention-name {
  flex: 1;
  font-size: 13px;
}

.mention-check {
  color: #1890FF;
}

/* 已选@标签 */
.mention-tags {
  padding: 4px 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  border-bottom: 1px solid #F5F5F5;
}

/* 输入区域 */
.chat-input {
  padding: 12px 20px;
  border-top: 1px solid #F0E6D8;
  background: white;
  position: relative;
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
  color: #8C8C8C;
}

/* 未选择群组 */
.no-group-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8C8C8C;
}

.no-group-selected .empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.no-group-selected h2 {
  margin: 0 0 8px;
  color: #262626;
}

/* Agent选择器 */
.agent-selector {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #F0E6D8;
  border-radius: 8px;
  padding: 8px;
}

.agent-checkbox-item {
  padding: 6px 0;
}

.agent-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}
</style>
