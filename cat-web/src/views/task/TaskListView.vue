<template>
  <div class="task-list">
    <div class="page-header">
      <h1 class="page-title">任务管理</h1>
      <el-button type="primary" @click="showCreateDialog = true">
        + 创建任务
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="searchKeyword" placeholder="搜索任务" style="width: 200px" @keyup.enter="loadTasks" />
      <el-select v-model="statusFilter" placeholder="状态" style="width: 120px" @change="loadTasks">
        <el-option label="全部" value="" />
        <el-option label="待执行" value="PENDING" />
        <el-option label="已分配" value="ASSIGNED" />
        <el-option label="执行中" value="RUNNING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="失败" value="FAILED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-select v-model="typeFilter" placeholder="类型" style="width: 140px" @change="loadTasks">
        <el-option label="全部" value="" />
        <el-option label="简单任务" value="SIMPLE" />
        <el-option label="流程任务" value="WORKFLOW" />
        <el-option label="并行任务" value="PARALLEL" />
        <el-option label="协商任务" value="NEGOTIATION" />
      </el-select>
      <el-button @click="loadTasks">刷新</el-button>
    </div>

    <el-table :data="tasks" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" label="任务名称" min-width="200" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="getTypeTagType(row.type)">{{ getTypeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <span :class="['status-tag', row.status.toLowerCase()]">{{ getStatusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="80">
        <template #default="{ row }">
          <span :class="['priority', `p${row.priority}`]">
            {{ ['低', '中', '高'][row.priority] || '中' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewTask(row.id)">详情</el-button>
          <el-button
            v-if="row.status === 'PENDING' || row.status === 'RUNNING'"
            size="small"
            type="danger"
            @click="handleCancelTask(row.id)"
          >取消</el-button>
          <el-button
            v-if="row.status === 'FAILED'"
            size="small"
            type="warning"
            @click="handleRetryTask(row.id)"
          >重试</el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDeleteTask(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadTasks"
      />
    </div>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建任务" width="600px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="createForm.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="简单任务" value="SIMPLE" />
            <el-option label="流程任务" value="WORKFLOW" />
            <el-option label="并行任务" value="PARALLEL" />
            <el-option label="协商任务" value="NEGOTIATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" style="width: 100%">
            <el-option label="低" :value="0" />
            <el-option label="中" :value="1" />
            <el-option label="高" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="输入数据">
          <el-input v-model="createInputJson" type="textarea" rows="4" placeholder='JSON格式，如: {"key": "value"}' />
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
  getTaskList,
  createTask,
  cancelTask,
  retryTask,
  deleteTask,
  type Task,
  type CreateTaskRequest,
  type TaskType,
  type TaskStatus
} from '@/api/task'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const showCreateDialog = ref(false)

const tasks = ref<Task[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createForm = ref<CreateTaskRequest>({
  name: '',
  type: 'SIMPLE',
  priority: 1,
  description: ''
})
const createInputJson = ref('')

onMounted(() => {
  loadTasks()
})

async function loadTasks() {
  loading.value = true
  try {
    const result = await getTaskList({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value as TaskStatus || undefined,
      type: typeFilter.value as TaskType || undefined,
      name: searchKeyword.value || undefined
    })
    tasks.value = result.items
    total.value = result.total
  } catch (error) {
    console.error('加载任务列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name) {
    ElMessage.warning('请输入任务名称')
    return
  }

  // 解析输入JSON
  if (createInputJson.value) {
    try {
      createForm.value.input = JSON.parse(createInputJson.value)
    } catch {
      ElMessage.warning('输入数据格式错误，请使用有效的JSON格式')
      return
    }
  }

  creating.value = true
  try {
    await createTask(createForm.value)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.value = { name: '', type: 'SIMPLE', priority: 1, description: '' }
    createInputJson.value = ''
    loadTasks()
  } catch (error) {
    console.error('创建任务失败:', error)
  } finally {
    creating.value = false
  }
}

function viewTask(id: string) {
  router.push(`/tasks/${id}`)
}

async function handleCancelTask(id: string) {
  try {
    await ElMessageBox.confirm('确定要取消该任务吗？', '确认', { type: 'warning' })
    await cancelTask(id)
    ElMessage.success('任务已取消')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error)
    }
  }
}

async function handleRetryTask(id: string) {
  try {
    await retryTask(id)
    ElMessage.success('任务已重新执行')
    loadTasks()
  } catch (error) {
    console.error('重试任务失败:', error)
  }
}

async function handleDeleteTask(task: Task) {
  try {
    await ElMessageBox.confirm(`确定要删除任务 "${task.name}" 吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteTask(task.id)
    ElMessage.success('删除成功')
    loadTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除任务失败:', error)
    }
  }
}

function getTypeText(type: TaskType): string {
  const typeMap: Record<TaskType, string> = {
    SIMPLE: '简单',
    WORKFLOW: '流程',
    PARALLEL: '并行',
    NEGOTIATION: '协商'
  }
  return typeMap[type] || type
}

function getTypeTagType(type: TaskType): string {
  const typeMap: Record<TaskType, string> = {
    SIMPLE: '',
    WORKFLOW: 'success',
    PARALLEL: 'warning',
    NEGOTIATION: 'info'
  }
  return typeMap[type] || ''
}

function getStatusText(status: TaskStatus): string {
  const statusMap: Record<TaskStatus, string> = {
    PENDING: '待执行',
    ASSIGNED: '已分配',
    RUNNING: '执行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return statusMap[status] || status
}

function formatTime(time: string): string {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.task-list {
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
.status-tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}
.status-tag.completed { background: #F6FFED; color: #52C41A; }
.status-tag.running { background: #E6F7FF; color: #1890FF; }
.status-tag.pending { background: #FFFBE6; color: #FAAD14; }
.status-tag.assigned { background: #F0F5FF; color: #2F54EB; }
.status-tag.failed { background: #FFF2F0; color: #FF4D4F; }
.status-tag.cancelled { background: #F5F5F5; color: #8C8C8C; }
.priority.p0 { color: #8C8C8C; }
.priority.p1 { color: #1890FF; }
.priority.p2 { color: #FF4D4F; font-weight: 600; }
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>