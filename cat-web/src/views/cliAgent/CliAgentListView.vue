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

    <!-- 创建Agent对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建CLI Agent" width="600px" :close-on-click-modal="false">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="选择模板" required>
          <el-select v-model="createForm.templateId" style="width: 100%" @change="onTemplateChange" placeholder="请选择CLI模板">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id">
              <span>{{ t.name }}</span>
              <span style="color: #999; margin-left: 10px;">{{ t.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="Agent名称" required>
          <el-input v-model="createForm.name" placeholder="请输入Agent名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">启动配置</el-divider>

        <el-form-item label="可执行路径">
          <el-input v-model="createForm.executablePath" placeholder="留空使用模板默认路径" />
        </el-form-item>
        <el-form-item label="配置文件">
          <el-input v-model="createForm.configPath" placeholder="可选，如 C:\Users\xxx\.claude\settings.json" />
          <div class="form-hint">指定配置文件路径，启动时将添加 --settings 参数</div>
        </el-form-item>
        <el-form-item label="启动参数">
          <el-select v-model="createForm.args" multiple filterable allow-create style="width: 100%" placeholder="添加启动参数">
          </el-select>
        </el-form-item>
        <el-form-item label="工作目录">
          <el-input v-model="createForm.workingDir" placeholder="CLI进程的工作目录" />
        </el-form-item>

        <el-divider content-position="left">环境变量</el-divider>

        <el-form-item label="">
          <div class="env-vars-editor">
            <div v-for="(item, index) in envVarList" :key="index" class="env-var-item">
              <el-input v-model="item.key" placeholder="变量名" style="width: 150px;" />
              <el-input v-model="item.value" placeholder="变量值" style="flex: 1;" show-password />
              <el-button type="danger" size="small" @click="envVarList.splice(index, 1)">删除</el-button>
            </div>
            <el-button type="primary" size="small" @click="envVarList.push({key: '', value: ''})">+ 添加环境变量</el-button>
          </div>
        </el-form-item>

        <el-divider content-position="left">能力配置</el-divider>

        <el-form-item label="能力类型">
          <el-select v-model="createForm.capabilityType" placeholder="选择能力类型" style="width: 200px;">
            <el-option v-for="ct in capabilityTypes" :key="ct.code" :label="ct.description" :value="ct.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="领域标签">
          <el-select v-model="createForm.domainTags" multiple filterable allow-create style="width: 100%" placeholder="输入领域标签，如java, python">
          </el-select>
        </el-form-item>
        <el-form-item label="熟练度">
          <el-rate v-model="createForm.proficiencyLevel" :max="5" show-score />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑Agent对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑CLI Agent" width="600px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="Agent名称" required>
          <el-input v-model="editForm.name" placeholder="请输入Agent名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" rows="2" placeholder="请输入描述" />
        </el-form-item>

        <el-divider content-position="left">启动配置</el-divider>

        <el-form-item label="可执行路径">
          <el-input v-model="editForm.executablePath" placeholder="留空使用模板默认路径" />
          <div class="form-hint">当前模板: {{ editingAgent?.templateName }}</div>
        </el-form-item>
        <el-form-item label="配置文件">
          <el-input v-model="editForm.configPath" placeholder="可选，如 C:\Users\xxx\.claude\settings.json" />
          <div class="form-hint">指定配置文件路径，启动时将添加 --settings 参数</div>
        </el-form-item>
        <el-form-item label="启动参数">
          <el-select v-model="editForm.args" multiple filterable allow-create style="width: 100%" placeholder="添加启动参数">
          </el-select>
        </el-form-item>
        <el-form-item label="工作目录">
          <el-input v-model="editForm.workingDir" placeholder="CLI进程的工作目录" />
        </el-form-item>

        <el-divider content-position="left">环境变量</el-divider>

        <el-form-item label="">
          <div class="env-vars-editor">
            <div v-for="(item, index) in editEnvVarList" :key="index" class="env-var-item">
              <el-input v-model="item.key" placeholder="变量名" style="width: 150px;" />
              <el-input v-model="item.value" placeholder="变量值" style="flex: 1;" show-password />
              <el-button type="danger" size="small" @click="editEnvVarList.splice(index, 1)">删除</el-button>
            </div>
            <el-button type="primary" size="small" @click="editEnvVarList.push({key: '', value: ''})">+ 添加环境变量</el-button>
          </div>
        </el-form-item>

        <el-divider content-position="left">能力配置</el-divider>

        <el-form-item label="能力类型">
          <el-select v-model="editForm.capabilityType" placeholder="选择能力类型" style="width: 200px;">
            <el-option v-for="ct in capabilityTypes" :key="ct.code" :label="ct.description" :value="ct.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="领域标签">
          <el-select v-model="editForm.domainTags" multiple filterable allow-create style="width: 100%" placeholder="输入领域标签">
          </el-select>
        </el-form-item>
        <el-form-item label="熟练度">
          <el-rate v-model="editForm.proficiencyLevel" :max="5" show-score />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTemplates,
  getAgents,
  getAgent,
  createAgent,
  updateAgent,
  startAgent,
  stopAgent,
  deleteAgent,
  getCapabilityTypes
} from '@/api/cliAgent'

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
const creating = ref(false)
const editing = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const templateFilter = ref('')
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const editingAgent = ref<CliAgent | null>(null)

const agents = ref<CliAgent[]>([])
const templates = ref<Template[]>([])
const capabilityTypes = ref<CapabilityType[]>([])

const envVarList = ref<{key: string, value: string}[]>([])
const editEnvVarList = ref<{key: string, value: string}[]>([])

const createForm = reactive({
  templateId: '',
  name: '',
  description: '',
  executablePath: '',
  configPath: '',
  args: [] as string[],
  workingDir: '',
  capabilityType: '',
  domainTags: [] as string[],
  proficiencyLevel: 3
})

const editForm = reactive({
  name: '',
  description: '',
  executablePath: '',
  configPath: '',
  args: [] as string[],
  workingDir: '',
  capabilityType: '',
  domainTags: [] as string[],
  proficiencyLevel: 3
})

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

function onTemplateChange(templateId: string) {
  const template = templates.value.find(t => t.id === templateId)
  if (template) {
    // 根据模板的必需环境变量自动添加环境变量条目
    envVarList.value = template.requiredEnvVars?.map(v => ({key: v, value: ''})) || []
  }
}

function openCreateDialog() {
  resetCreateForm()
  showCreateDialog.value = true
}

function openEditDialog(agent: CliAgent) {
  editingAgent.value = agent

  // 填充编辑表单
  editForm.name = agent.name
  editForm.description = agent.description || ''
  editForm.executablePath = agent.executablePath || ''
  editForm.configPath = agent.configPath || ''
  editForm.args = agent.args || []
  editForm.workingDir = agent.workingDir || ''

  // 填充环境变量
  editEnvVarList.value = []
  if (agent.envVars) {
    Object.entries(agent.envVars).forEach(([key, value]) => {
      editEnvVarList.value.push({ key, value: value || '' })
    })
  }

  // 填充能力配置
  if (agent.capabilities && agent.capabilities.length > 0) {
    const cap = agent.capabilities[0]
    editForm.capabilityType = cap.type || ''
    editForm.domainTags = cap.domainTags || []
    editForm.proficiencyLevel = cap.proficiencyLevel || 3
  } else {
    editForm.capabilityType = ''
    editForm.domainTags = []
    editForm.proficiencyLevel = 3
  }

  showEditDialog.value = true
}

async function handleCreate() {
  if (!createForm.templateId) {
    ElMessage.warning('请选择模板')
    return
  }
  if (!createForm.name) {
    ElMessage.warning('请输入Agent名称')
    return
  }

  // 构建环境变量
  const envVars: Record<string, string> = {}
  envVarList.value.forEach(item => {
    if (item.key && item.value) {
      envVars[item.key] = item.value
    }
  })

  creating.value = true
  try {
    const payload: any = {
      name: createForm.name,
      description: createForm.description,
      templateId: createForm.templateId
    }

    if (createForm.executablePath) payload.executablePath = createForm.executablePath
    if (createForm.configPath) payload.configPath = createForm.configPath
    if (createForm.args.length > 0) payload.args = createForm.args
    if (createForm.workingDir) payload.workingDir = createForm.workingDir
    if (Object.keys(envVars).length > 0) payload.envVars = envVars

    if (createForm.capabilityType) {
      payload.capabilities = [{
        type: createForm.capabilityType,
        domainTags: createForm.domainTags,
        proficiencyLevel: createForm.proficiencyLevel
      }]
    }

    await createAgent(payload)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    loadAgents()
  } catch (error: any) {
    console.error('创建Agent失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function handleEdit() {
  if (!editForm.name) {
    ElMessage.warning('请输入Agent名称')
    return
  }

  if (!editingAgent.value) return

  // 构建环境变量
  const envVars: Record<string, string> = {}
  editEnvVarList.value.forEach(item => {
    if (item.key && item.value) {
      envVars[item.key] = item.value
    }
  })

  editing.value = true
  try {
    const payload: any = {
      name: editForm.name,
      description: editForm.description
    }

    if (editForm.executablePath) payload.executablePath = editForm.executablePath
    if (editForm.configPath) payload.configPath = editForm.configPath
    if (editForm.args.length > 0) payload.args = editForm.args
    if (editForm.workingDir) payload.workingDir = editForm.workingDir
    if (Object.keys(envVars).length > 0) payload.envVars = envVars

    if (editForm.capabilityType) {
      payload.capabilities = [{
        type: editForm.capabilityType,
        domainTags: editForm.domainTags,
        proficiencyLevel: editForm.proficiencyLevel
      }]
    }

    await updateAgent(editingAgent.value.id, payload)
    ElMessage.success('保存成功')
    showEditDialog.value = false
    loadAgents()
  } catch (error: any) {
    console.error('保存Agent失败:', error)
    ElMessage.error(error.message || '保存失败')
  } finally {
    editing.value = false
  }
}

function resetCreateForm() {
  createForm.templateId = ''
  createForm.name = ''
  createForm.description = ''
  createForm.executablePath = ''
  createForm.configPath = ''
  createForm.args = []
  createForm.workingDir = ''
  createForm.capabilityType = ''
  createForm.domainTags = []
  createForm.proficiencyLevel = 3
  envVarList.value = []
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

<style scoped>
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
  font-size: 24px;
  font-weight: 600;
  margin: 0;
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
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
  border: 1px solid #F0E6D8;
}
.agent-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}
.agent-icon {
  font-size: 32px;
}
.agent-info {
  flex: 1;
}
.agent-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
}
.agent-desc {
  font-size: 13px;
  color: #8C8C8C;
  margin: 0;
}
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status-tag.running { background: #F6FFED; color: #52C41A; }
.status-tag.executing { background: #E6F7FF; color: #1890FF; }
.status-tag.stopped { background: #F5F5F5; color: #8C8C8C; }
.status-tag.error { background: #FFF1F0; color: #FF4D4F; }
.status-tag.starting { background: #FFFBE6; color: #FAAD14; }
.agent-meta {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
.agent-stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid #F0E6D8;
  border-bottom: 1px solid #F0E6D8;
  margin-bottom: 16px;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat .value {
  font-size: 14px;
  font-weight: 600;
  color: #FF8C42;
}
.stat .label {
  font-size: 12px;
  color: #8C8C8C;
}
.agent-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.env-vars-editor {
  width: 100%;
}
.env-var-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.form-hint {
  font-size: 12px;
  color: #8C8C8C;
  margin-top: 4px;
}
</style>