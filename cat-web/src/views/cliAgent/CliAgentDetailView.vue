<template>
  <div class="cli-agent-detail" v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="router.back()" text>
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h1 class="page-title">{{ agent?.name || 'CLI Agent详情' }}</h1>
        <el-tag :type="getStatusType(agent?.status)">{{ getStatusText(agent?.status) }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="handleStart" :loading="starting" :disabled="agent?.status === 'RUNNING' || agent?.status === 'EXECUTING'">启动</el-button>
        <el-button type="warning" @click="handleStop" :loading="stopping" :disabled="agent?.status === 'STOPPED'">停止</el-button>
        <el-button @click="handleRestart" :loading="restarting">重启</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：基本信息和进程状态 -->
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <span>基本信息</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Agent ID">{{ agent?.id }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ agent?.name }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ agent?.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="模板">{{ agent?.templateName }}</el-descriptions-item>
            <el-descriptions-item label="CLI类型">{{ agent?.cliType }}</el-descriptions-item>
            <el-descriptions-item label="可执行路径">{{ agent?.executablePath || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作目录">{{ agent?.workingDir || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(agent?.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="process-card">
          <template #header>
            <div class="card-header">
              <span>进程状态</span>
              <el-button size="small" @click="loadProcessStatus">刷新</el-button>
            </div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="模式">
              <el-tag type="info">每请求模式 (--print)</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="PID">
              <el-tag v-if="processStatus?.processId" type="success">{{ processStatus?.processId }}</el-tag>
              <el-tag v-else type="info">无持久进程</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">{{ processStatus?.status || '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ formatUptime(processStatus?.uptimeMs) }}</el-descriptions-item>
            <el-descriptions-item label="启动时间">{{ formatTime(processStatus?.startTime ? new Date(processStatus.startTime).toISOString() : null) }}</el-descriptions-item>
            <el-descriptions-item label="Session ID">
              <el-tag v-if="processStatus?.sessionId" type="success" size="small">{{ processStatus?.sessionId?.substring(0, 8) }}...</el-tag>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="session-card">
          <template #header>
            <span>会话状态</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="活跃">
              <el-tag :type="sessionStatus?.active ? 'success' : 'info'">{{ sessionStatus?.active ? '是' : '否' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理中">
              <el-tag :type="sessionStatus?.active ? 'primary' : 'info'">{{ sessionStatus?.active ? '是' : '否' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="接收行数">{{ sessionStatus?.linesReceived || 0 }}</el-descriptions-item>
            <el-descriptions-item label="发送字节">{{ sessionStatus?.bytesSent || 0 }}</el-descriptions-item>
            <el-descriptions-item label="最后错误">
              <span style="color: #f56c6c;">{{ sessionStatus?.lastError || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="token-card">
          <template #header>
            <div class="card-header">
              <span>Token统计</span>
              <el-button size="small" @click="loadTokenStats">刷新</el-button>
            </div>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="输入Token">{{ formatNumber(tokenStats?.totalInputTokens) }}</el-descriptions-item>
            <el-descriptions-item label="输出Token">{{ formatNumber(tokenStats?.totalOutputTokens) }}</el-descriptions-item>
            <el-descriptions-item label="总计">{{ formatNumber(tokenStats?.totalTokens) }}</el-descriptions-item>
            <el-descriptions-item label="记录数">{{ tokenStats?.recordCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="首次记录">{{ formatTime(tokenStats?.firstRecordTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后记录">{{ formatTime(tokenStats?.lastRecordTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：输出日志 -->
      <el-col :span="16">
        <el-card class="communication-card">
          <template #header>
            <div class="card-header">
              <span>输出日志</span>
              <div class="header-actions">
                <el-button size="small" @click="loadOutputLogs">刷新</el-button>
                <el-button size="small" type="danger" @click="handleClearLogs">清空</el-button>
              </div>
            </div>
          </template>

          <!-- 输出区域 -->
          <div class="output-area" ref="outputRef">
            <div v-for="(line, index) in outputLines" :key="index" :class="['output-line', line.type]">
              <span class="timestamp">{{ line.time }}</span>
              <span class="content">{{ line.content }}</span>
            </div>
            <el-empty v-if="outputLines.length === 0" description="暂无输出日志" :image-size="60" />
          </div>
        </el-card>

        <el-card class="capabilities-card">
          <template #header>
            <div class="card-header">
              <span>能力配置</span>
              <el-button size="small" type="primary" @click="showAddCapability = true">添加能力</el-button>
            </div>
          </template>
          <el-table :data="capabilities" size="small" v-if="capabilities.length > 0">
            <el-table-column prop="type" label="类型" width="120" />
            <el-table-column prop="typeName" label="类型名称" width="100" />
            <el-table-column label="领域标签">
              <template #default="{ row }">
                <el-tag v-for="tag in row.domainTags" :key="tag" size="small" style="margin-right: 4px;">{{ tag }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="proficiencyLevel" label="熟练度" width="100">
              <template #default="{ row }">
                <el-rate v-model="row.proficiencyLevel" disabled :max="5" size="small" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无能力配置" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加能力对话框 -->
    <el-dialog v-model="showAddCapability" title="添加能力" width="400px">
      <el-form :model="capabilityForm" label-width="80px">
        <el-form-item label="能力类型">
          <el-select v-model="capabilityForm.type" style="width: 100%">
            <el-option v-for="ct in capabilityTypes" :key="ct.code" :label="ct.description" :value="ct.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="领域标签">
          <el-select v-model="capabilityForm.domainTags" multiple filterable allow-create style="width: 100%" placeholder="输入标签">
          </el-select>
        </el-form-item>
        <el-form-item label="熟练度">
          <el-rate v-model="capabilityForm.proficiencyLevel" :max="5" show-score />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddCapability = false">取消</el-button>
        <el-button type="primary" @click="handleAddCapability">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getAgent,
  startAgent,
  stopAgent,
  restartAgent,
  getAgentStatus,
  getSessionStatus,
  getAgentTokenStats,
  getAgentCapabilities,
  addCapability,
  getCapabilityTypes,
  getOutputLogs,
  clearOutputLogs
} from '@/api/cliAgent'

interface Agent {
  id: string
  name: string
  description: string
  templateName: string
  cliType: string
  status: string
  executablePath: string
  workingDir: string
  createdAt: string
}

interface OutputLine {
  type: 'output' | 'error' | 'system'
  time: string
  content: string
}

const router = useRouter()
const route = useRoute()
const agentId = route.params.id as string

const loading = ref(true)
const agent = ref<Agent | null>(null)

// 进程控制
const starting = ref(false)
const stopping = ref(false)
const restarting = ref(false)

// 状态信息
const processStatus = ref<any>(null)
const sessionStatus = ref<any>(null)
const tokenStats = ref<any>(null)
const capabilities = ref<any[]>([])
const capabilityTypes = ref<any[]>([])

// 输出日志
const outputLines = ref<OutputLine[]>([])
const outputRef = ref<HTMLElement | null>(null)

// 能力
const showAddCapability = ref(false)
const capabilityForm = ref({
  type: '',
  domainTags: [] as string[],
  proficiencyLevel: 3
})

let statusTimer: any = null

onMounted(async () => {
  await loadAgent()
  await loadCapabilities()
  await loadCapabilityTypes()
  await loadOutputLogs()
  startStatusPolling()
})

onUnmounted(() => {
  stopStatusPolling()
})

async function loadAgent() {
  loading.value = true
  try {
    agent.value = await getAgent(agentId)
  } catch (error) {
    ElMessage.error('加载Agent信息失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

async function loadCapabilities() {
  try {
    capabilities.value = await getAgentCapabilities(agentId) || []
  } catch (error) {
    console.error('加载能力失败:', error)
  }
}

async function loadCapabilityTypes() {
  try {
    capabilityTypes.value = await getCapabilityTypes() || []
  } catch (error) {
    console.error('加载能力类型失败:', error)
  }
}

async function loadProcessStatus() {
  try {
    processStatus.value = await getAgentStatus(agentId)
  } catch (error) {
    console.error('加载进程状态失败:', error)
  }
}

async function loadSessionStatus() {
  try {
    sessionStatus.value = await getSessionStatus(agentId)
  } catch (error) {
    console.error('加载会话状态失败:', error)
  }
}

async function loadTokenStats() {
  try {
    tokenStats.value = await getAgentTokenStats(agentId)
  } catch (error) {
    console.error('加载Token统计失败:', error)
  }
}

function startStatusPolling() {
  loadProcessStatus()
  loadSessionStatus()
  loadTokenStats()
  statusTimer = setInterval(() => {
    loadProcessStatus()
    loadSessionStatus()
    loadTokenStats()
  }, 5000)
}

function stopStatusPolling() {
  if (statusTimer) {
    clearInterval(statusTimer)
    statusTimer = null
  }
}

async function handleStart() {
  starting.value = true
  try {
    await startAgent(agentId)
    ElMessage.success('启动成功')
    await loadAgent()
    addOutputLine('system', '进程已启动')
  } catch (error: any) {
    ElMessage.error(error.message || '启动失败')
  } finally {
    starting.value = false
  }
}

async function handleStop() {
  stopping.value = true
  try {
    await stopAgent(agentId)
    ElMessage.success('已停止')
    await loadAgent()
    addOutputLine('system', '进程已停止')
  } catch (error: any) {
    ElMessage.error(error.message || '停止失败')
  } finally {
    stopping.value = false
  }
}

async function handleRestart() {
  restarting.value = true
  try {
    await restartAgent(agentId)
    ElMessage.success('重启成功')
    await loadAgent()
    addOutputLine('system', '进程已重启')
  } catch (error: any) {
    ElMessage.error(error.message || '重启失败')
  } finally {
    restarting.value = false
  }
}

async function loadOutputLogs() {
  try {
    const logs = await getOutputLogs(agentId, 100)
    outputLines.value = logs.map((log: any) => ({
      type: log.type === 'error' ? 'error' : log.type === 'text' ? 'output' : 'system',
      time: new Date(log.timestamp).toLocaleTimeString(),
      content: log.content
    }))
    nextTick(() => {
      if (outputRef.value) {
        outputRef.value.scrollTop = outputRef.value.scrollHeight
      }
    })
  } catch (error) {
    console.error('加载输出日志失败:', error)
  }
}

async function handleClearLogs() {
  try {
    await clearOutputLogs(agentId)
    outputLines.value = []
    ElMessage.success('日志已清空')
  } catch (error: any) {
    ElMessage.error(error.message || '清空失败')
  }
}

function addOutputLine(type: 'output' | 'error' | 'system', content: string) {
  const time = new Date().toLocaleTimeString()
  outputLines.value.push({ type, time, content })
  nextTick(() => {
    if (outputRef.value) {
      outputRef.value.scrollTop = outputRef.value.scrollHeight
    }
  })
}

async function handleAddCapability() {
  if (!capabilityForm.value.type) {
    ElMessage.warning('请选择能力类型')
    return
  }
  try {
    await addCapability(agentId, capabilityForm.value)
    ElMessage.success('添加成功')
    showAddCapability.value = false
    capabilityForm.value = { type: '', domainTags: [], proficiencyLevel: 3 }
    loadCapabilities()
  } catch (error: any) {
    ElMessage.error(error.message || '添加失败')
  }
}

function getStatusType(status: string | undefined): string {
  const map: Record<string, string> = {
    RUNNING: 'success',
    EXECUTING: 'primary',
    STOPPED: 'info',
    ERROR: 'danger',
    STARTING: 'warning'
  }
  return map[status || ''] || 'info'
}

function getStatusText(status: string | undefined): string {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    EXECUTING: '执行中',
    STOPPED: '已停止',
    ERROR: '错误',
    STARTING: '启动中'
  }
  return map[status || ''] || status || '-'
}

function formatTime(time: string | null | undefined): string {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

function formatUptime(ms: number | null | undefined): string {
  if (!ms) return '-'
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  if (hours > 0) return `${hours}小时${minutes % 60}分`
  if (minutes > 0) return `${minutes}分${seconds % 60}秒`
  return `${seconds}秒`
}

function formatNumber(num: number | null | undefined): string {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString('zh-CN')
}
</script>

<style scoped>
.cli-agent-detail {
  padding: 0;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.info-card, .process-card, .session-card, .token-card, .communication-card, .capabilities-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.output-area {
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 8px;
  padding: 12px;
  height: 300px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.5;
}
.output-line {
  margin-bottom: 4px;
}
.output-line .timestamp {
  color: #6a9955;
  margin-right: 8px;
}
.output-line.output .content {
  color: #d4d4d4;
}
.output-line.error .content {
  color: #f14c4c;
}
.output-line.system .content {
  color: #569cd6;
}
.input-area {
  margin-top: 16px;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.hint {
  color: #999;
  font-size: 12px;
}
</style>