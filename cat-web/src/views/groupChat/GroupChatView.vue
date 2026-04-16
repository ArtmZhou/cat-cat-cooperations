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
            <!-- 自动讨论开关 -->
            <div class="auto-discussion-controls">
              <el-tooltip content="开启后Agent会自动互相讨论/博弈" placement="bottom">
                <el-switch
                  v-model="autoDiscussionEnabled"
                  active-text="自动讨论"
                  inactive-text=""
                  size="small"
                  @change="handleAutoDiscussionToggle"
                />
              </el-tooltip>
              <el-tooltip v-if="autoDiscussionEnabled" content="自动讨论最大轮数" placement="bottom">
                <el-input-number
                  v-model="maxAutoRounds"
                  :min="1"
                  :max="20"
                  size="small"
                  controls-position="right"
                  class="rounds-input"
                  @change="handleMaxRoundsChange"
                />
              </el-tooltip>
              <el-button
                v-if="autoDiscussionRunning"
                type="danger"
                size="small"
                @click="handleStopAutoDiscussion"
                class="stop-discussion-btn"
              >
                🛑 中断讨论
              </el-button>
            </div>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-chat">
            <div class="empty-icon">💬</div>
            <p>开始群聊吧！</p>
            <p class="hint">发送消息给所有Agent，或在输入框中输入 @ 指定Agent</p>
            <p v-if="autoDiscussionEnabled" class="hint">💡 自动讨论模式已开启，Agent会通过@互相回应、讨论</p>
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

          <!-- 自动讨论进行中指示器 -->
          <div v-if="autoDiscussionRunning && Object.keys(activeOutputs).length === 0" class="auto-discussion-indicator">
            <div class="discussion-pulse"></div>
            <span>🔄 自动讨论进行中... 等待被@的Agent回应</span>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <!-- @提及选择器 -->
          <div v-if="showMentionPopup" class="mention-popup">
            <div class="mention-header">
              {{ mentionAtPosition >= 0 ? '输入名称筛选Agent：' : '选择要@的Agent：' }}
            </div>
            <div
              v-for="(agent, idx) in (mentionAtPosition >= 0 ? filteredMentionAgents : selectedGroup.agents)"
              :key="agent.id"
              :class="['mention-item', {
                selected: mentionedAgentIds.includes(agent.id),
                highlighted: mentionAtPosition >= 0 && idx === mentionHighlightIndex
              }]"
              @click="mentionAtPosition >= 0 ? selectMentionFromInput(agent.id) : toggleMention(agent.id)"
            >
              <span class="mention-avatar">🤖</span>
              <span class="mention-name">{{ agent.name }}</span>
              <el-tag size="small" :type="getAgentTagType(agent.status)">
                {{ getAgentStatusText(agent.status) }}
              </el-tag>
              <el-icon v-if="mentionedAgentIds.includes(agent.id)" class="mention-check"><Check /></el-icon>
            </div>
            <div v-if="mentionAtPosition >= 0 && filteredMentionAgents.length === 0" class="mention-empty">
              无匹配的Agent
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
        <el-form-item label="自动讨论">
          <div class="auto-discussion-form">
            <el-switch
              v-model="groupForm.autoDiscussion"
              active-text="开启"
              inactive-text="关闭"
            />
            <div v-if="groupForm.autoDiscussion" class="auto-discussion-desc">
              <p>开启后，Agent通过@指定成员来互相回应和讨论，只有被@的Agent才会响应</p>
              <el-form-item label="最大轮数" class="rounds-form-item">
                <el-input-number
                  v-model="groupForm.maxAutoRounds"
                  :min="1"
                  :max="20"
                  size="small"
                />
                <span class="rounds-hint">每次用户发言后，Agent自动讨论的最大轮数</span>
              </el-form-item>
            </div>
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
  clearGroupMessages,
  stopAutoDiscussion
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
  autoDiscussion: boolean
  maxAutoRounds: number
  autoDiscussionRunning: boolean
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
const inputRef = ref<any>(null)

// @mention state
const showMentionPopup = ref(false)
const mentionedAgentIds = ref<string[]>([])
const mentionFilter = ref('')  // 输入框中@后的过滤文本
const mentionAtPosition = ref(-1) // @符号在输入框中的位置
const mentionHighlightIndex = ref(0) // 键盘导航高亮索引

// Streaming output state: agentId -> accumulated text
const activeOutputs = ref<Record<string, string>>({})

// Auto-discussion state
const autoDiscussionEnabled = ref(false)
const autoDiscussionRunning = ref(false)
const maxAutoRounds = ref(6)

// All agents (for group creation)
const allAgents = ref<AgentBrief[]>([])

// Group form
const showCreateDialog = ref(false)
const editingGroup = ref<ChatGroup | null>(null)
const savingGroup = ref(false)
const groupForm = ref({
  name: '',
  description: '',
  agentIds: [] as string[],
  autoDiscussion: false,
  maxAutoRounds: 6
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
  return '输入消息广播给所有Agent... (Enter发送，输入@指定Agent)'
})

// 根据@后输入的文本过滤Agent列表
const filteredMentionAgents = computed(() => {
  if (!selectedGroup.value?.agents) return []
  const filter = mentionFilter.value.toLowerCase()
  if (!filter) return selectedGroup.value.agents
  return selectedGroup.value.agents.filter(a =>
    a.name.toLowerCase().includes(filter)
  )
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
  autoDiscussionRunning.value = false
  closeMentionPopupFromInput()
})

// 监听输入文本变化，检测@触发
watch(inputText, () => {
  // 使用nextTick确保DOM更新后获取正确的cursor位置
  nextTick(() => handleInputChange())
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
  // 同步自动讨论状态
  autoDiscussionEnabled.value = group.autoDiscussion || false
  maxAutoRounds.value = group.maxAutoRounds || 6
  autoDiscussionRunning.value = group.autoDiscussionRunning || false
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

  // Subscribe to auto-discussion status changes
  cliWebSocket.subscribeGroupAutoDiscussionStatus(groupId, (data: any) => {
    console.log('Auto-discussion status:', data)
    autoDiscussionRunning.value = data.running || false
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

// 处理输入框中检测@符号
function handleInputChange() {
  // 从el-input ref获取底层textarea元素
  const textareaEl = inputRef.value?.$el?.querySelector('textarea') as HTMLTextAreaElement | null
  if (!textareaEl) return

  const value = inputText.value
  const cursorPos = textareaEl.selectionStart || value.length

  // 查找光标前最近的@符号
  const textBeforeCursor = value.substring(0, cursorPos)
  const lastAtIndex = textBeforeCursor.lastIndexOf('@')

  if (lastAtIndex >= 0) {
    // @符号前必须是空格、换行或者在行首
    const charBefore = lastAtIndex > 0 ? textBeforeCursor[lastAtIndex - 1] : ' '
    if (charBefore === ' ' || charBefore === '\n' || lastAtIndex === 0) {
      const query = textBeforeCursor.substring(lastAtIndex + 1)
      // @后面不能有空格（空格表示@完成）
      if (!query.includes(' ') && !query.includes('\n')) {
        mentionFilter.value = query
        mentionAtPosition.value = lastAtIndex
        mentionHighlightIndex.value = 0
        showMentionPopup.value = true
        return
      }
    }
  }

  // 没有匹配的@，关闭通过输入触发的弹窗
  if (mentionAtPosition.value >= 0) {
    closeMentionPopupFromInput()
  }
}

// 处理输入框键盘事件（用于@弹窗的键盘导航）
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

  // 原有的Enter发送逻辑
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 从输入框@弹窗中选择Agent
function selectMentionFromInput(agentId: string) {
  // 将agent加入mentioned列表
  if (!mentionedAgentIds.value.includes(agentId)) {
    mentionedAgentIds.value.push(agentId)
  }

  // 移除输入框中的@query文本
  const atPos = mentionAtPosition.value
  if (atPos >= 0) {
    const before = inputText.value.substring(0, atPos)
    const afterCursor = inputText.value.substring(atPos + 1 + mentionFilter.value.length)
    inputText.value = before + afterCursor
  }

  closeMentionPopupFromInput()
}

// 关闭由输入触发的@弹窗
function closeMentionPopupFromInput() {
  mentionFilter.value = ''
  mentionAtPosition.value = -1
  mentionHighlightIndex.value = 0
  showMentionPopup.value = false
}

// @按钮切换弹窗（手动模式）
function toggleMentionPopupButton() {
  // 如果当前是输入触发的弹窗，先关闭
  if (mentionAtPosition.value >= 0) {
    closeMentionPopupFromInput()
    return
  }
  showMentionPopup.value = !showMentionPopup.value
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
      // 刷新selectedGroup如果正在编辑当前选中的群组
      if (selectedGroup.value?.id === editingGroup.value.id && updated) {
        selectedGroup.value = updated as ChatGroup
      }
    } else {
      await createChatGroup(groupForm.value)
      ElMessage.success('群组已创建')
    }

    showCreateDialog.value = false
    editingGroup.value = null
    groupForm.value = { name: '', description: '', agentIds: [], autoDiscussion: false, maxAutoRounds: 6 }
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
      agentIds: [...group.agentIds],
      autoDiscussion: group.autoDiscussion || false,
      maxAutoRounds: group.maxAutoRounds || 6
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

// ===== Auto-Discussion =====
async function handleAutoDiscussionToggle(value: boolean) {
  if (!selectedGroup.value) return
  try {
    const updated = await updateChatGroup(selectedGroup.value.id, {
      ...selectedGroup.value,
      autoDiscussion: value,
      maxAutoRounds: maxAutoRounds.value
    })
    if (updated) {
      selectedGroup.value = updated as ChatGroup
    }
    ElMessage.success(value ? '自动讨论已开启' : '自动讨论已关闭')
  } catch (error: any) {
    ElMessage.error(error.message || '设置失败')
    autoDiscussionEnabled.value = !value
  }
}

async function handleMaxRoundsChange(value: number) {
  if (!selectedGroup.value) return
  try {
    await updateChatGroup(selectedGroup.value.id, {
      ...selectedGroup.value,
      autoDiscussion: autoDiscussionEnabled.value,
      maxAutoRounds: value
    })
  } catch (error: any) {
    ElMessage.error(error.message || '设置失败')
  }
}

async function handleStopAutoDiscussion() {
  if (!selectedGroup.value) return
  try {
    await stopAutoDiscussion(selectedGroup.value.id)
    autoDiscussionRunning.value = false
    ElMessage.success('已中断自动讨论')
  } catch (error: any) {
    ElMessage.error(error.message || '中断失败')
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
  // 先转义HTML特殊字符，防止XSS（&必须最先转义，避免double-escape）
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

// ===== Left sidebar =====
.chat-sidebar {
  width: 280px;
  background: $bg-deep;
  border-right: 1px solid $border-subtle;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    font-size: 16px;
    color: $text-primary;
  }
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
  border-bottom: 1px solid rgba(124, 58, 237, 0.05);
  transition: background 0.2s;
  position: relative;

  &:hover {
    background: $bg-hover;
  }

  &.active {
    background: $bg-surface;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 8px;
      bottom: 8px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: linear-gradient(180deg, $color-violet, $color-cyan);
    }
  }
}

.group-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-weight: 600;
  font-size: 14px;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  font-size: 12px;
  color: $text-muted;
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

// ===== Messages =====
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

// ===== @Mention Popup =====
.mention-popup {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: $bg-elevated;
  border: 1px solid $border-active;
  border-radius: $radius-md;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.3);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.mention-header {
  padding: 8px 12px;
  font-size: 12px;
  color: $text-muted;
  border-bottom: 1px solid $border-subtle;
}

.mention-item {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.2s;
  color: $text-secondary;

  &:hover {
    background: $bg-hover;
  }

  &.selected {
    background: $color-violet-dim;
  }

  &.highlighted {
    background: $color-violet-dim;
    outline: 1px solid $color-violet;
    outline-offset: -1px;
  }
}

.mention-empty {
  padding: 12px;
  text-align: center;
  color: $text-muted;
  font-size: 13px;
}

.mention-avatar {
  font-size: 16px;
}

.mention-name {
  flex: 1;
  font-size: 13px;
  color: $text-primary;
}

.mention-check {
  color: $color-violet;
}

// ===== Mention Tags =====
.mention-tags {
  padding: 4px 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  border-bottom: 1px solid $border-subtle;
}

// ===== Chat Input =====
.chat-input {
  padding: 12px 20px;
  border-top: 1px solid $border-subtle;
  background: $bg-surface;
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
  color: $text-muted;
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

// ===== Agent Selector (in dialog) =====
.agent-selector {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  padding: 8px;
  background: $bg-surface;
}

.agent-checkbox-item {
  padding: 6px 0;
}

.agent-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

// ===== Auto-Discussion Controls =====
.auto-discussion-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rounds-input {
  width: 100px;
}

.stop-discussion-btn {
  animation: pulse-glow 2s infinite;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 4px rgba(245, 108, 108, 0.3);
  }
  50% {
    box-shadow: 0 0 12px rgba(245, 108, 108, 0.6);
  }
}

// ===== Auto-Discussion Indicator =====
.auto-discussion-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin: 8px 0;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.06), rgba(6, 182, 212, 0.06));
  border: 1px solid rgba(124, 58, 237, 0.15);
  border-radius: $radius-md;
  font-size: 13px;
  color: $text-secondary;

  .discussion-pulse {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: $color-violet;
    animation: discussion-pulse-anim 1.5s infinite;
  }
}

@keyframes discussion-pulse-anim {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

// ===== Auto-Discussion Form (in dialog) =====
.auto-discussion-form {
  width: 100%;
}

.auto-discussion-desc {
  margin-top: 8px;
  padding: 10px;
  background: $bg-hover;
  border-radius: $radius-sm;
  font-size: 12px;
  color: $text-muted;

  p {
    margin: 0 0 8px;
  }
}

.rounds-form-item {
  margin-top: 8px;
  margin-bottom: 0;
}

.rounds-hint {
  font-size: 12px;
  color: $text-muted;
  margin-left: 8px;
}
</style>
