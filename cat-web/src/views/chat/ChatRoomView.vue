<template>
  <div class="chat-room">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>CLI Agents</h3>
        <el-button size="small" @click="loadAgents" :loading="loading">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>

      <div class="agent-list">
        <div
          v-for="agent in agents"
          :key="agent.id"
          :class="['agent-item', { active: selectedAgent?.id === agent.id }]"
          @click="selectAgent(agent)"
        >
          <div class="agent-avatar">🤖</div>
          <div class="agent-info">
            <div class="agent-name">{{ agent.name }}</div>
            <div class="agent-status">
              <span :class="['status-dot', getStatusClass(agent.status)]"></span>
              <span>{{ getStatusText(agent.status) }}</span>
            </div>
          </div>
          <div class="agent-actions" v-if="agent.status !== 'RUNNING' && agent.status !== 'EXECUTING'">
            <el-button
              size="small"
              type="success"
              @click.stop="handleStartAgent(agent)"
              :loading="agent._starting"
            >启动</el-button>
          </div>
        </div>

        <el-empty v-if="agents.length === 0" description="暂无Agent，请先创建" :image-size="60" />
      </div>
    </div>

    <div class="chat-main">
      <template v-if="selectedAgent">
        <div class="chat-header">
          <div class="header-info">
            <span class="agent-avatar-large">🤖</span>
            <div class="header-text">
              <h2>{{ selectedAgent.name }}</h2>
              <p>{{ selectedAgent.templateName }} · {{ selectedAgent.cliType }}</p>
            </div>
          </div>
          <div class="header-actions">
            <el-tag :type="getStatusType(selectedAgent.status)">
              {{ getStatusText(selectedAgent.status) }}
            </el-tag>
            <el-button
              v-if="selectedAgent.status === 'RUNNING' || selectedAgent.status === 'EXECUTING'"
              type="warning"
              size="small"
              @click="handleStopAgent"
            >停止</el-button>
            <el-button
              v-if="selectedAgent.status === 'STOPPED' || selectedAgent.status === 'ERROR'"
              type="success"
              size="small"
              @click="handleStartAgent(selectedAgent)"
            >启动</el-button>
            <el-button size="small" @click="clearChat">清空对话</el-button>
          </div>
        </div>

        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-chat">
            <div class="empty-icon">💬</div>
            <p>开始与 {{ selectedAgent.name }} 对话吧！</p>
            <p class="hint">发送任务或问题，Agent将为您处理</p>
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message', msg.role]"
          >
            <div class="message-avatar">
              {{ msg.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="sender">{{ msg.role === 'user' ? '我' : selectedAgent.name }}</span>
                <span class="time">{{ msg.time }}</span>
              </div>
              <div class="message-text" v-html="formatMessage(msg.content)"></div>
              <div v-if="msg.tokens" class="message-tokens">
                <el-tag size="small" type="info">输入: {{ msg.tokens.input }} | 输出: {{ msg.tokens.output }}</el-tag>
              </div>
            </div>
          </div>

          <!-- 实时状态指示器 -->
          <div v-if="cliStatus" class="cli-status-bar">
            <div class="status-spinner">
              <span class="spinner-icon">{{ spinnerFrame }}</span>
            </div>
            <div class="status-text">
              <span class="status-action">{{ cliStatus.action || '处理中' }}</span>
              <span v-if="cliStatus.detail" class="status-detail">{{ cliStatus.detail }}</span>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入任务或问题... (Shift+Enter换行，Enter发送)"
            @keydown.enter.exact.prevent="sendMessage"
            :disabled="!isAgentReady"
          />
          <div class="input-actions">
            <div class="input-hints">
              <span v-if="!isAgentReady" class="warning">⚠️ Agent未运行，请先启动</span>
              <span v-else>按 Enter 发送，Shift+Enter 换行</span>
            </div>
            <el-button
              type="primary"
              @click="sendMessage"
              :loading="isLoading"
              :disabled="!inputText.trim() || !isAgentReady"
            >
              发送
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="no-agent-selected">
        <div class="empty-icon">👈</div>
        <h2>选择一个Agent开始对话</h2>
        <p>从左侧列表中选择一个CLI Agent</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAgents,
  getAgent,
  startAgent,
  stopAgent,
  sendInput,
  getSessionStatus,
  getAgentTokenStats
} from '@/api/cliAgent'
import { cliWebSocket } from '@/utils/websocket'

interface Agent {
  id: string
  name: string
  templateName: string
  cliType: string
  status: string
  _starting?: boolean
}

interface Message {
  role: 'user' | 'assistant'
  content: string
  time: string
  tokens?: { input: number; output: number }
}

const loading = ref(false)
const agents = ref<Agent[]>([])
const selectedAgent = ref<Agent | null>(null)
const messages = ref<Message[]>([])
const inputText = ref('')
const isLoading = ref(false)
const messagesRef = ref<HTMLElement | null>(null)
const wsConnected = ref(false)

// CLI 状态指示器
const cliStatus = ref<{ action: string; detail?: string } | null>(null)
const spinnerFrame = ref('⠋')
const spinnerFrames = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']
let spinnerInterval: ReturnType<typeof setInterval> | null = null

// 启动 spinner 动画
function startSpinner() {
  if (spinnerInterval) return
  let i = 0
  spinnerInterval = setInterval(() => {
    spinnerFrame.value = spinnerFrames[i % spinnerFrames.length]
    i++
  }, 80)
}

// 停止 spinner 动画
function stopSpinner() {
  if (spinnerInterval) {
    clearInterval(spinnerInterval)
    spinnerInterval = null
  }
}

// 解析 CLI 输出行，区分状态和内容
function parseCliOutput(line: string): { isStatus: boolean; action?: string; detail?: string; content?: string } {
  // 移除 ANSI 转义码
  const cleanLine = line.replace(/\x1b\[[0-9;]*[a-zA-Z]/g, '').trim()

  // 检测 spinner 模式（⠋⠙⠹ 等动画字符）
  if (/^[⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏]/.test(cleanLine)) {
    const match = cleanLine.match(/^[⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏]\s*(.+)/)
    if (match) {
      return { isStatus: true, action: match[1] }
    }
  }

  // 检测工具调用状态
  if (cleanLine.startsWith('✓') || cleanLine.startsWith('✔')) {
    return { isStatus: true, action: '完成', detail: cleanLine.substring(1).trim() }
  }
  if (cleanLine.startsWith('✗') || cleanLine.startsWith('✘')) {
    return { isStatus: true, action: '失败', detail: cleanLine.substring(1).trim() }
  }
  if (cleanLine.startsWith('⟳')) {
    return { isStatus: true, action: '重试', detail: cleanLine.substring(1).trim() }
  }

  // 检测 "Thinking..." 或 "Processing..." 等状态
  if (/^(Thinking|Processing|Analyzing|Reading|Writing|Executing|Running|Loading|Fetching)\s*\.\.\./i.test(cleanLine)) {
    return { isStatus: true, action: cleanLine }
  }

  // 检测工具名称（通常是独立一行的工具调用）
  if (/^(Bash|Read|Write|Edit|Grep|Glob|WebSearch|WebFetch|Task|Agent|Skill)\s*\(/.test(cleanLine)) {
    return { isStatus: true, action: '执行工具', detail: cleanLine }
  }

  // 其他情况视为实际内容
  return { isStatus: false, content: line }
}

// Agent是否准备好（运行中）
const isAgentReady = computed(() => {
  return selectedAgent.value && selectedAgent.value.status === 'RUNNING'
})

// 消息历史缓存（按agentId）
const messageHistory = ref<Record<string, Message[]>>({})

// localStorage key
const STORAGE_KEY = 'cli-agent-chat-history'
const SELECTED_AGENT_KEY = 'cli-agent-selected-agent-id'

// 当前正在接收的消息内容（按agentId）
const currentOutputs = ref<Record<string, string>>({})

// 获取当前agent的currentOutput
const currentOutput = computed(() => {
  if (!selectedAgent.value) return ''
  return currentOutputs.value[selectedAgent.value.id] || ''
})

// 设置当前agent的currentOutput
function setCurrentOutput(value: string) {
  if (!selectedAgent.value) return
  currentOutputs.value[selectedAgent.value.id] = value
}

// 从 localStorage 加载消息历史
function loadMessageHistory() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      messageHistory.value = JSON.parse(saved)
    }
  } catch (e) {
    console.error('Failed to load message history:', e)
  }
}

// 保存消息历史到 localStorage
function saveMessageHistory() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(messageHistory.value))
  } catch (e) {
    console.error('Failed to save message history:', e)
  }
}

// 保存选中的Agent ID到localStorage
function saveSelectedAgentId(agentId: string | null) {
  try {
    if (agentId) {
      localStorage.setItem(SELECTED_AGENT_KEY, agentId)
    } else {
      localStorage.removeItem(SELECTED_AGENT_KEY)
    }
  } catch (e) {
    console.error('Failed to save selected agent id:', e)
  }
}

// 恢复上次选中的Agent
function restoreSelectedAgent() {
  try {
    const savedAgentId = localStorage.getItem(SELECTED_AGENT_KEY)
    if (savedAgentId && agents.value.length > 0) {
      const agent = agents.value.find(a => a.id === savedAgentId)
      if (agent) {
        selectedAgent.value = { ...agent }
        messages.value = messageHistory.value[agent.id] || []
        console.log('Restored selected agent:', agent.id, 'messages:', messages.value.length)
        nextTick(() => scrollToBottom())
      }
    }
  } catch (e) {
    console.error('Failed to restore selected agent:', e)
  }
}

onMounted(async () => {
  // 加载消息历史
  loadMessageHistory()

  await loadAgents()

  // 恢复上次选中的Agent
  restoreSelectedAgent()

  // 连接WebSocket
  try {
    await cliWebSocket.connect()
    wsConnected.value = true
    console.log('WebSocket connected')

    // 如果已经有选中的Agent，立即订阅
    if (selectedAgent.value) {
      subscribeToAgent(selectedAgent.value.id)
    }
  } catch (error) {
    console.error('WebSocket connection failed:', error)
  }
})

onUnmounted(() => {
  // 停止 spinner
  stopSpinner()

  // 保存当前消息历史
  if (selectedAgent.value && messages.value.length > 0) {
    messageHistory.value[selectedAgent.value.id] = [...messages.value]
    saveMessageHistory()
  }

  // 取消订阅
  if (selectedAgent.value) {
    cliWebSocket.unsubscribe(selectedAgent.value.id)
  }
  // 不断开连接，让其他组件可以继续使用
})

// 监听selectedAgent变化
watch(selectedAgent, (newAgent, oldAgent) => {
  // 取消旧Agent的订阅
  if (oldAgent) {
    cliWebSocket.unsubscribe(oldAgent.id)
    // 注意：不重置currentOutputs，保留正在接收的消息
  }

  if (newAgent) {
    // 加载该agent的消息历史
    messages.value = messageHistory.value[newAgent.id] || []
    // 恢复该agent的currentOutput（不重置）
    console.log('Switched to agent:', newAgent.id, 'loaded messages:', messages.value.length, 'currentOutput:', currentOutputs.value[newAgent.id]?.length || 0)

    // 更新agent状态
    refreshAgentStatus()

    // 订阅WebSocket输出
    if (wsConnected.value) {
      subscribeToAgent(newAgent.id)
    }
  }
})

async function loadAgents() {
  loading.value = true
  try {
    const result = await getAgents({ page: 1, pageSize: 100 })
    agents.value = result.items || []
  } catch (error) {
    console.error('加载Agent列表失败:', error)
    ElMessage.error('加载Agent列表失败')
  } finally {
    loading.value = false
  }
}

// 订阅Agent的WebSocket输出
function subscribeToAgent(agentId: string) {
  // 先取消之前的订阅（如果有）
  cliWebSocket.unsubscribe(agentId)

  console.log('Subscribing to agent:', agentId)

  // 订阅标准输出
  cliWebSocket.subscribeOutput(agentId, (data) => {
    console.log('Received output for agent', agentId, ':', data)
    if (!data.type) return

    // 无论当前是否选中该agent，都保存输出
    const isSelected = selectedAgent.value?.id === agentId

    if (data.type === 'output' && data.content) {
      const parsed = parseCliOutput(data.content)

      if (parsed.isStatus) {
        // 只有选中的agent才更新状态指示器
        if (isSelected) {
          cliStatus.value = { action: parsed.action || '', detail: parsed.detail }
          startSpinner()
        }
      } else if (parsed.content) {
        // 停止状态指示器（如果是选中agent）
        if (isSelected) {
          stopSpinner()
          cliStatus.value = null
        }

        // 追加到当前输出（按agent存储）- 无论是否选中都保存
        const current = currentOutputs.value[agentId] || ''
        if (current === '') {
          currentOutputs.value[agentId] = parsed.content
        } else {
          currentOutputs.value[agentId] = current + '\n' + parsed.content
        }

        // 只有选中的agent才更新UI
        if (isSelected) {
          updateAssistantMessage(currentOutputs.value[agentId], agentId)
        } else {
          // 未选中的agent也更新历史（但不更新UI）
          updateAssistantMessage(currentOutputs.value[agentId], agentId)
        }
      }
    } else if (data.type === 'text_delta') {
      // 流式文本片段（--print模式）或完整响应
      if (isSelected) {
        stopSpinner()
        cliStatus.value = null
      }

      // 追加文本（如果内容不为空）- 无论是否选中都保存
      if (data.content) {
        const current = currentOutputs.value[agentId] || ''
        currentOutputs.value[agentId] = current + data.content
        // 更新历史（传入agentId）
        updateAssistantMessage(currentOutputs.value[agentId], agentId)
      }
    } else if (data.type === 'done') {
      // 响应完成信号
      if (isSelected) {
        stopSpinner()
        cliStatus.value = null
        isLoading.value = false

        // 如果当前输出为空，显示提示
        if (!currentOutputs.value[agentId]) {
          updateAssistantMessage('⚠️ 未收到响应内容', agentId)
        }
      }
      // 完成后清空当前输出缓存
      delete currentOutputs.value[agentId]
    }
  })

  // 订阅错误输出
  cliWebSocket.subscribeError(agentId, (data) => {
    console.log('Received error:', data)
    stopSpinner()
    cliStatus.value = null
    isLoading.value = false
    if (data.type === 'error' && data.content) {
      addMessage('assistant', '⚠️ ' + data.content, agentId)
    }
  })

  // 订阅状态变化
  cliWebSocket.subscribeStatus(agentId, (data) => {
    console.log('Status change:', data)
    if (selectedAgent.value && data.status) {
      selectedAgent.value.status = data.status
      // 更新列表中的状态
      const index = agents.value.findIndex(a => a.id === agentId)
      if (index !== -1) {
        agents.value[index].status = data.status
      }
    }
  })
}

// 更新或添加助手消息
function updateAssistantMessage(content: string, targetAgentId?: string) {
  // 收到实际内容，停止状态指示器（如果当前选中了该agent）
  const agentId = targetAgentId || selectedAgent.value?.id
  if (!agentId) return

  const isSelected = selectedAgent.value?.id === agentId

  if (isSelected) {
    stopSpinner()
    cliStatus.value = null
    isLoading.value = false
  }

  // 获取该agent的消息列表
  const agentMessages = messageHistory.value[agentId] || []

  // 查找最后一条助手消息
  const lastMsg = agentMessages[agentMessages.length - 1]
  if (lastMsg && lastMsg.role === 'assistant') {
    // 更新最后一条消息
    agentMessages[agentMessages.length - 1] = {
      ...lastMsg,
      content: content,
      time: new Date().toLocaleTimeString()
    }
  } else {
    // 添加新消息
    agentMessages.push({
      role: 'assistant',
      content: content,
      time: new Date().toLocaleTimeString()
    })
  }

  // 更新历史
  messageHistory.value[agentId] = [...agentMessages]
  saveMessageHistory()
  console.log('Updated history for agent:', agentId, 'content length:', content.length)

  // 只有当前选中的agent才更新UI
  if (isSelected) {
    messages.value = [...agentMessages]
    nextTick(() => {
      scrollToBottom()
    })
  }
}

async function refreshAgentStatus() {
  if (!selectedAgent.value) return

  try {
    const updated = await getAgent(selectedAgent.value.id)
    selectedAgent.value.status = updated.status

    // 同时更新列表中的状态
    const index = agents.value.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.value[index].status = updated.status
    }
  } catch (error) {
    console.error('刷新Agent状态失败:', error)
  }
}

function selectAgent(agent: Agent) {
  // 保存当前agent的消息历史
  if (selectedAgent.value && messages.value.length > 0) {
    messageHistory.value[selectedAgent.value.id] = [...messages.value]
    saveMessageHistory() // 关键修复：添加保存
    console.log('Saved history for agent:', selectedAgent.value.id, 'messages:', messages.value.length)
  }

  // 切换前清空当前agent的输出缓存（只清空当前选中的，不是全局）
  if (selectedAgent.value) {
    delete currentOutputs.value[selectedAgent.value.id]
  }

  selectedAgent.value = { ...agent }
  messages.value = messageHistory.value[agent.id] || []
  // 保存选中的Agent ID到localStorage
  saveSelectedAgentId(agent.id)
  console.log('Loaded history for agent:', agent.id, 'messages:', messages.value.length)

  nextTick(() => {
    scrollToBottom()
  })
}

async function handleStartAgent(agent: Agent) {
  agent._starting = true
  try {
    const result = await startAgent(agent.id)
    agent.status = result.status
    if (selectedAgent.value?.id === agent.id) {
      selectedAgent.value.status = result.status
    }
    ElMessage.success('Agent已启动')

    // 确保WebSocket连接并订阅
    if (!wsConnected.value) {
      try {
        await cliWebSocket.connect()
        wsConnected.value = true
      } catch (e) {
        console.error('WebSocket连接失败:', e)
      }
    }

    // 如果是当前选中的agent，订阅输出
    if (selectedAgent.value?.id === agent.id && wsConnected.value) {
      subscribeToAgent(agent.id)
    }

    // 添加系统消息
    addMessage('assistant', '✅ Agent已启动，可以开始对话了！', agent.id)
  } catch (error: any) {
    ElMessage.error(error.message || '启动失败')
  } finally {
    agent._starting = false
  }
}

async function handleStopAgent() {
  if (!selectedAgent.value) return

  try {
    const result = await stopAgent(selectedAgent.value.id)
    selectedAgent.value.status = result.status

    // 更新列表中的状态
    const index = agents.value.findIndex(a => a.id === selectedAgent.value!.id)
    if (index !== -1) {
      agents.value[index].status = result.status
    }

    ElMessage.success('Agent已停止')
    addMessage('assistant', '⏹️ Agent已停止', selectedAgent.value.id)
  } catch (error: any) {
    ElMessage.error(error.message || '停止失败')
  }
}

async function sendMessage() {
  if (!inputText.value.trim() || !selectedAgent.value || !isAgentReady.value) return

  const agentId = selectedAgent.value.id
  const text = inputText.value.trim()
  inputText.value = ''

  // 添加用户消息（确保添加到正确的agent历史）
  addMessage('user', text, agentId)

  // 预添加一条空的助手消息（用于后续更新，确保添加到正确的agent历史）
  addMessage('assistant', '', agentId)

  // 重置状态（按agent）
  currentOutputs.value[agentId] = ''
  cliStatus.value = { action: '发送请求...' }
  startSpinner()
  isLoading.value = true

  try {
    // 发送输入到CLI
    const success = await sendInput(agentId, text)

    if (!success) {
      stopSpinner()
      cliStatus.value = null
      // 更新最后一条空消息为错误信息
      const lastMsg = messages.value[messages.value.length - 1]
      if (lastMsg && lastMsg.role === 'assistant') {
        lastMsg.content = '❌ 发送失败，请检查Agent状态'
      }
      isLoading.value = false
    }
    // 成功发送后，状态将通过 WebSocket 更新
  } catch (error: any) {
    stopSpinner()
    cliStatus.value = null
    // 更新最后一条空消息为错误信息
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'assistant') {
      lastMsg.content = `❌ 发送失败: ${error.message || '未知错误'}`
    }
    isLoading.value = false
  }
}

function addMessage(role: 'user' | 'assistant', content: string, targetAgentId?: string) {
  const agentId = targetAgentId || selectedAgent.value?.id
  if (!agentId) return

  const msg: Message = {
    role,
    content,
    time: new Date().toLocaleTimeString()
  }

  // 更新该agent的消息历史
  const agentMessages = messageHistory.value[agentId] || []
  agentMessages.push(msg)
  messageHistory.value[agentId] = [...agentMessages]
  saveMessageHistory()
  console.log('Added message for agent:', agentId, 'role:', role, 'content length:', content.length)

  // 只有当前选中的agent才更新UI
  if (selectedAgent.value?.id === agentId) {
    messages.value = [...agentMessages]
    nextTick(() => {
      scrollToBottom()
    })
  }
}

function clearChat() {
  messages.value = []
  if (selectedAgent.value) {
    messageHistory.value[selectedAgent.value.id] = []
    saveMessageHistory()
  }
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

function formatMessage(content: string): string {
  // 简单的markdown格式化
  return content
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
}

function getStatusClass(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'running',
    EXECUTING: 'executing',
    STOPPED: 'stopped',
    ERROR: 'error'
  }
  return map[status] || 'stopped'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    EXECUTING: '执行中',
    STOPPED: '已停止',
    ERROR: '错误',
    STARTING: '启动中'
  }
  return map[status] || status
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'success',
    EXECUTING: 'primary',
    STOPPED: 'info',
    ERROR: 'danger',
    STARTING: 'warning'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.chat-room {
  display: flex;
  height: calc(100vh - 64px - 48px);
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
}

/* 左侧Agent列表 */
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

.agent-list {
  flex: 1;
  overflow-y: auto;
}

.agent-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #F5F5F5;
  transition: background 0.2s;
}

.agent-item:hover {
  background: #FFF9F5;
}

.agent-item.active {
  background: #FFF5E6;
  border-left: 3px solid #FF8C42;
}

.agent-avatar {
  width: 40px;
  height: 40px;
  background: #FFF5E6;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-weight: 600;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-status {
  font-size: 12px;
  color: #8C8C8C;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.running { background: #52C41A; }
.status-dot.executing { background: #1890FF; }
.status-dot.stopped { background: #8C8C8C; }
.status-dot.error { background: #FF4D4F; }

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid #F0E6D8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #FAFAFA;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agent-avatar-large {
  font-size: 36px;
}

.header-text h2 {
  margin: 0;
  font-size: 18px;
}

.header-text p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #8C8C8C;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
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
  margin-bottom: 20px;
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

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.sender {
  font-weight: 600;
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: #BFBFBF;
}

.message-text {
  background: white;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid #F0E6D8;
  word-break: break-word;
}

.message.user .message-text {
  background: #FFF5E6;
  border-color: #FFE8CC;
}

.message-tokens {
  margin-top: 8px;
}

/* 加载动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #F0E6D8;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #FF8C42;
  border-radius: 50%;
  animation: typing 1.4s infinite both;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* 输入区域 */
.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #F0E6D8;
  background: white;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.input-hints {
  font-size: 12px;
  color: #8C8C8C;
}

.input-hints .warning {
  color: #FAAD14;
}

/* 未选择Agent */
.no-agent-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8C8C8C;
}

.no-agent-selected .empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.no-agent-selected h2 {
  margin: 0 0 8px;
  color: #262626;
}

/* CLI 状态栏 */
.cli-status-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin: 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  animation: pulse 2s infinite;
}

.status-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner-icon {
  font-size: 18px;
  font-weight: bold;
}

.status-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-action {
  font-weight: 600;
  font-size: 14px;
}

.status-detail {
  font-size: 12px;
  opacity: 0.9;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}
</style>