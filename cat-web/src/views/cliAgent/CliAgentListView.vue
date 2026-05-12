<template>
  <div class="cli-agent-list">
    <div class="page-header">
      <h1 class="page-title">CLI Agent管理</h1>
      <el-button type="primary" @click="openCreateDialog">
        + 创建CLI Agent
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索Agent名称" style="width: 200px" @keyup.enter="loadAgents" />
      <el-select v-model="statusFilter" placeholder="状态" style="width: 120px" @change="loadAgents" clearable>
        <el-option label="全部" value="" />
        <el-option label="运行中" value="RUNNING" />
        <el-option label="执行中" value="EXECUTING" />
        <el-option label="已停止" value="STOPPED" />
        <el-option label="错误" value="ERROR" />
      </el-select>
      <el-select v-model="templateFilter" placeholder="模板" style="width: 150px" @change="loadAgents" clearable>
        <el-option label="全部" value="" />
        <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
      <el-button @click="loadAgents">刷新</el-button>
    </div>

    <div v-loading="loading" class="agent-grid">
      <div v-for="agent in agents" :key="agent.id" class="agent-card">
        <div class="agent-header">
          <span class="agent-icon">🤖</span>
          <div class="agent-info">
            <h3 class="agent-name">{{ agent.name }}</h3>
            <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>
          </div>
          <span :class="['status-tag', getStatusClass(agent.status)]">{{ getStatusText(agent.status) }}</span>
        </div>

        <div class="agent-meta">
          <el-tag size="small" type="info">{{ agent.templateName }}</el-tag>
          <el-tag size="small" v-if="agent.cliType">{{ agent.cliType }}</el-tag>
        </div>

        <div class="agent-stats">
          <div class="stat">
            <span class="value">{{ agent.processId || '-' }}</span>
            <span class="label">PID</span>
          </div>
          <div class="stat">
            <span class="value">{{ agent.capabilities?.length || 0 }}</span>
            <span class="label">能力数</span>
          </div>
          <div class="stat">
            <span class="value">{{ formatTime(agent.lastStartedAt) }}</span>
            <span class="label">最后启动</span>
          </div>
        </div>

        <div class="agent-actions">
          <el-button size="small" @click="viewAgent(agent.id)">详情</el-button>
          <el-button
            size="small"
            @click="openEditDialog(agent)"
          >编辑</el-button>
          <el-button
            size="small"
            type="success"
            v-if="agent.status === 'STOPPED' || agent.status === 'ERROR'"
            @click="handleStart(agent)"
            :loading="agent._starting"
          >启动</el-button>
          <el-button
            size="small"
            type="warning"
            v-if="agent.status === 'RUNNING' || agent.status === 'EXECUTING'"
            @click="handleStop(agent)"
            :loading="agent._stopping"
          >停止</el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(agent)"
          >删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && agents.length === 0" description="暂无CLI Agent，点击右上角创建" />
    </div>

    <CliAgentCreateDialog
      v-model:visible="showCreateDialog"
      :templates="templates"
      :capability-types="capabilityTypes"
      @created="onCreated"
    />
    <CliAgentEditDialog
      v-model:visible="showEditDialog"
      :agent="editingAgent"
      :templates="templates"
      :capability-types="capabilityTypes"
      @updated="onUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTemplates,
  getAgents,
  startAgent,
  stopAgent,
  deleteAgent,
  getCapabilityTypes
} from '@/api/cliAgent'
import CliAgentCreateDialog from './CliAgentCreateDialog.vue'
import CliAgentEditDialog from './CliAgentEditDialog.vue'

interface CliAgent {
  id: string
  name: string
  description: string
  templateId: string
  templateName: string
  cliType: string
  status: string
  executablePath: string
  configPath: string
  args: string[]
  envVars: Record<string, string>
  workingDir: string
  processId: string
  capabilities: any[]
  lastStartedAt: string
  _starting?: boolean
  _stopping?: boolean
}

interface Template {
  id: string
  name: string
  description: string
  cliType: string
  requiredEnvVars: string[]
}

interface CapabilityType {
  code: string
  name: string
  description: string
}

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const templateFilter = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editingAgent = ref<CliAgent | null>(null)

const agents = ref<CliAgent[]>([])
const templates = ref<Template[]>([])
const capabilityTypes = ref<CapabilityType[]>([])

onMounted(async () => {
  await Promise.all([
    loadAgents(),
    loadTemplates(),
    loadCapabilityTypes()
  ])
})

async function loadAgents() {
  loading.value = true
  try {
    const result = await getAgents({
      page: 1,
      pageSize: 100,
      status: statusFilter.value || undefined,
      templateId: templateFilter.value || undefined,
      name: searchKeyword.value || undefined
    })
    agents.value = result.items || []
  } catch (error) {
    console.error('加载Agent列表失败:', error)
    ElMessage.error('加载Agent列表失败')
  } finally {
    loading.value = false
  }
}

async function loadTemplates() {
  try {
    const result = await getTemplates()
    templates.value = result || []
  } catch (error) {
    console.error('加载模板列表失败:', error)
  }
}

async function loadCapabilityTypes() {
  try {
    const result = await getCapabilityTypes()
    capabilityTypes.value = result || []
  } catch (error) {
    console.error('加载能力类型失败:', error)
  }
}

function openCreateDialog() {
  showCreateDialog.value = true
}

function openEditDialog(agent: CliAgent) {
  editingAgent.value = agent
  showEditDialog.value = true
}

function onCreated() {
  loadAgents()
}

function onUpdated() {
  loadAgents()
}

function viewAgent(id: string) {
  router.push(`/cli-agents/${id}`)
}

async function handleStart(agent: CliAgent) {
  agent._starting = true
  try {
    await startAgent(agent.id)
    ElMessage.success('启动成功')
    loadAgents()
  } catch (error: any) {
    ElMessage.error(error.message || '启动失败')
  } finally {
    agent._starting = false
  }
}

async function handleStop(agent: CliAgent) {
  agent._stopping = true
  try {
    await stopAgent(agent.id)
    ElMessage.success('已停止')
    loadAgents()
  } catch (error: any) {
    ElMessage.error(error.message || '停止失败')
  } finally {
    agent._stopping = false
  }
}

async function handleDelete(agent: CliAgent) {
  try {
    await ElMessageBox.confirm(`确定要删除Agent "${agent.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    await deleteAgent(agent.id)
    ElMessage.success('删除成功')
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

function getStatusClass(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'running',
    EXECUTING: 'executing',
    STOPPED: 'stopped',
    ERROR: 'error',
    STARTING: 'starting'
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

function formatTime(time: string): string {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.cli-agent-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  min-height: 200px;
}

.agent-card {
  background: $bg-surface;
  border-radius: $radius-md;
  padding: 20px;
  border: 1px solid $border-subtle;
  transition: all 0.25s ease;

  &:hover {
    border-color: $border-active;
    box-shadow: $glow-violet;
    transform: translateY(-1px);
  }
}

.agent-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.agent-icon {
  font-size: 28px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: $text-primary;
}

.agent-desc {
  font-size: 13px;
  color: $text-secondary;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  padding: 3px 10px;
  border-radius: $radius-sm;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-tag.running {
  background: rgba(6, 182, 212, 0.12);
  color: $status-running;
  animation: breathe 2s ease-in-out infinite;
}

.status-tag.executing {
  background: rgba(124, 58, 237, 0.12);
  color: $status-executing;
  animation: pulse-ring 1.5s ease-out infinite;
}

.status-tag.stopped {
  background: rgba(75, 85, 99, 0.15);
  color: $status-stopped;
}

.status-tag.error {
  background: rgba(239, 68, 68, 0.12);
  color: $status-error;
}

.status-tag.starting {
  background: rgba(245, 158, 11, 0.12);
  color: $warning;
}

.agent-meta {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.agent-stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid $border-subtle;
  border-bottom: 1px solid $border-subtle;
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat .value {
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat .label {
  font-size: 12px;
  color: $text-muted;
}

.agent-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
