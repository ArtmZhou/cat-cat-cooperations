<template>
  <div class="agent-list">
    <div class="page-header">
      <h1 class="page-title">Agent管理</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        + 创建Agent
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索Agent" style="width: 200px" @keyup.enter="loadAgents" />
      <el-select v-model="statusFilter" placeholder="状态" style="width: 120px" @change="loadAgents">
        <el-option label="全部" value="" />
        <el-option label="在线" value="ONLINE" />
        <el-option label="离线" value="OFFLINE" />
        <el-option label="忙碌" value="BUSY" />
      </el-select>
      <el-button @click="loadAgents">刷新</el-button>
    </div>

    <div v-loading="loading" class="agent-grid">
      <div v-for="agent in agents" :key="agent.id" class="agent-card">
        <div class="agent-header">
          <span class="agent-icon">🐱</span>
          <div class="agent-info">
            <h3 class="agent-name">{{ agent.name }}</h3>
            <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>
          </div>
          <span :class="['status-tag', agent.status.toLowerCase()]">{{ getStatusText(agent.status) }}</span>
        </div>

        <div class="agent-meta">
          <span class="meta-item">
            <el-tag size="small" :type="agent.type === 'BUILT_IN' ? 'primary' : 'success'">
              {{ agent.type === 'BUILT_IN' ? '内置' : '外部' }}
            </el-tag>
          </span>
        </div>

        <div class="agent-stats">
          <div class="stat">
            <span class="value">{{ agent.lastHeartbeat ? formatTime(agent.lastHeartbeat) : '-' }}</span>
            <span class="label">最后心跳</span>
          </div>
          <div class="stat">
            <span class="value">{{ formatTime(agent.createdAt) }}</span>
            <span class="label">创建时间</span>
          </div>
        </div>

        <div class="agent-actions">
          <el-button size="small" @click="viewAgent(agent.id)">详情</el-button>
          <el-button
            size="small"
            :type="agent.status === 'DISABLED' ? 'success' : 'warning'"
            @click="toggleAgentStatus(agent)"
          >
            {{ agent.status === 'DISABLED' ? '启用' : '禁用' }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDeleteAgent(agent)"
          >删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && agents.length === 0" description="暂无Agent数据" />
    </div>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadAgents"
      />
    </div>

    <!-- 创建Agent对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建Agent" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="请输入Agent名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="内置Agent" value="BUILT_IN" />
            <el-option label="外部Agent" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAgentList,
  createAgent,
  enableAgent,
  disableAgent,
  deleteAgent,
  type Agent,
  type CreateAgentRequest
} from '@/api/agent'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const showCreateDialog = ref(false)

const agents = ref<Agent[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const createForm = ref<CreateAgentRequest>({
  name: '',
  type: 'BUILT_IN',
  description: ''
})

onMounted(() => {
  loadAgents()
})

async function loadAgents() {
  loading.value = true
  try {
    const result = await getAgentList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined,
      name: searchKeyword.value || undefined
    })
    agents.value = result.items
    total.value = result.total
  } catch (error) {
    console.error('加载Agent列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name) {
    ElMessage.warning('请输入Agent名称')
    return
  }

  creating.value = true
  try {
    await createAgent(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', type: 'BUILT_IN', description: '' }
    loadAgents()
  } catch (error) {
    console.error('创建Agent失败:', error)
  } finally {
    creating.value = false
  }
}

function viewAgent(id: string) {
  router.push(`/agents/${id}`)
}

async function toggleAgentStatus(agent: Agent) {
  try {
    if (agent.status === 'DISABLED') {
      await enableAgent(agent.id)
      ElMessage.success('已启用')
    } else {
      await disableAgent(agent.id)
      ElMessage.success('已禁用')
    }
    loadAgents()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

async function handleDeleteAgent(agent: Agent) {
  try {
    await ElMessageBox.confirm(`确定要删除Agent "${agent.name}" 吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteAgent(agent.id)
    ElMessage.success('删除成功')
    loadAgents()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除Agent失败:', error)
    }
  }
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    BUSY: '忙碌',
    ERROR: '错误',
    DISABLED: '禁用'
  }
  return statusMap[status] || status
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
.agent-list {
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
.status-tag.online { background: #F6FFED; color: #52C41A; }
.status-tag.offline { background: #F5F5F5; color: #8C8C8C; }
.status-tag.busy { background: #FFFBE6; color: #FAAD14; }
.status-tag.error { background: #FFF1F0; color: #FF4D4F; }
.status-tag.disabled { background: #F5F5F5; color: #8C8C8C; }
.agent-meta {
  margin-bottom: 12px;
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
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>