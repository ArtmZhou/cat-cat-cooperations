<template>
  <div class="workspace-list">
    <div class="page-header">
      <h1 class="page-title">工作空间管理</h1>
      <p class="page-subtitle">基于Git Worktree的多Agent并发开发工作空间</p>
      <el-button type="primary" @click="openCreateDialog">
        + 创建工作空间
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="projectPathFilter" placeholder="项目路径" style="width: 300px" @keyup.enter="loadWorkspaces" />
      <el-select v-model="statusFilter" placeholder="状态" style="width: 120px" @change="loadWorkspaces" clearable>
        <el-option label="全部" value="" />
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="已提交" value="COMMITTED" />
        <el-option label="已合并" value="MERGED" />
        <el-option label="已删除" value="REMOVED" />
        <el-option label="错误" value="ERROR" />
      </el-select>
      <el-button @click="loadWorkspaces">🔄 刷新</el-button>
    </div>

    <div v-loading="loading" class="workspace-grid">
      <div v-if="filteredWorkspaces.length === 0 && !loading" class="empty-state">
        <p>暂无工作空间，点击"创建工作空间"开始多Agent并发开发</p>
      </div>

      <div v-for="ws in filteredWorkspaces" :key="ws.id" class="workspace-card">
        <div class="workspace-header">
          <span class="workspace-icon">🌿</span>
          <div class="workspace-info">
            <h3 class="workspace-name">{{ ws.branchName }}</h3>
            <p class="workspace-desc">{{ ws.description || '无描述' }}</p>
          </div>
          <span :class="['status-tag', getStatusClass(ws.status)]">{{ getStatusText(ws.status) }}</span>
        </div>

        <div class="workspace-meta">
          <div class="meta-item">
            <span class="meta-label">项目:</span>
            <span class="meta-value">{{ ws.projectPath }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Worktree:</span>
            <span class="meta-value">{{ ws.worktreePath }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">基线分支:</span>
            <el-tag size="small" type="info">{{ ws.baseBranch }}</el-tag>
          </div>
          <div class="meta-item" v-if="ws.taskId">
            <span class="meta-label">任务ID:</span>
            <span class="meta-value">{{ ws.taskId }}</span>
          </div>
          <div class="meta-item" v-if="ws.agentId">
            <span class="meta-label">Agent:</span>
            <span class="meta-value">{{ ws.agentId }}</span>
          </div>
          <div class="meta-item" v-if="ws.lastCommitHash">
            <span class="meta-label">最近提交:</span>
            <span class="meta-value commit-hash">{{ ws.lastCommitHash?.substring(0, 8) }}</span>
            <span class="meta-value" v-if="ws.lastCommitMessage"> - {{ ws.lastCommitMessage }}</span>
          </div>
        </div>

        <div class="workspace-actions">
          <el-button size="small" @click="viewGitStatus(ws.id)" :disabled="ws.status === 'REMOVED'">
            📊 Git状态
          </el-button>
          <el-button size="small" type="success" @click="openCommitDialog(ws.id)" :disabled="ws.status === 'REMOVED'">
            ✅ 提交
          </el-button>
          <el-button size="small" type="primary" @click="handlePush(ws.id)" :disabled="ws.status === 'REMOVED'">
            🚀 推送
          </el-button>
          <el-button size="small" type="warning" @click="openMergeDialog(ws.id)" :disabled="ws.status === 'REMOVED'">
            🔀 合并
          </el-button>
          <el-button size="small" @click="handleSync(ws.id, ws.baseBranch)" :disabled="ws.status === 'REMOVED'">
            🔄 同步
          </el-button>
          <el-button size="small" type="danger" @click="handleRemove(ws.id)" :disabled="ws.status === 'REMOVED'">
            🗑️ 删除
          </el-button>
        </div>

        <div class="workspace-time">
          创建于: {{ formatTime(ws.createdAt) }}
          <span v-if="ws.updatedAt"> | 更新于: {{ formatTime(ws.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <!-- 创建工作空间对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建工作空间" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="项目路径" required>
          <el-input v-model="createForm.projectPath" placeholder="/path/to/your/project" />
        </el-form-item>
        <el-form-item label="分支名称">
          <el-input v-model="createForm.branchName" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="基线分支">
          <el-input v-model="createForm.baseBranch" placeholder="main" />
        </el-form-item>
        <el-form-item label="关联任务ID">
          <el-input v-model="createForm.taskId" placeholder="可选" />
        </el-form-item>
        <el-form-item label="关联Agent">
          <el-input v-model="createForm.agentId" placeholder="可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" placeholder="工作空间用途描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 提交对话框 -->
    <el-dialog v-model="commitDialogVisible" title="提交变更" width="400px">
      <el-form :model="commitForm" label-width="80px">
        <el-form-item label="提交信息" required>
          <el-input v-model="commitForm.message" type="textarea" :rows="3" placeholder="请输入提交信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCommit" :loading="committing">提交</el-button>
      </template>
    </el-dialog>

    <!-- 合并对话框 -->
    <el-dialog v-model="mergeDialogVisible" title="合并分支" width="400px">
      <el-form :model="mergeForm" label-width="80px">
        <el-form-item label="目标分支" required>
          <el-input v-model="mergeForm.targetBranch" placeholder="main" />
        </el-form-item>
      </el-form>
      <div style="margin-bottom: 16px">
        <el-button @click="handleCheckConflicts" :loading="checkingConflicts">🔍 先检测冲突</el-button>
        <span v-if="conflictResult" :style="{ color: conflictResult.hasConflicts ? 'red' : 'green', marginLeft: '8px' }">
          {{ conflictResult.hasConflicts ? `存在冲突 (${conflictResult.conflictFiles.length}个文件)` : '无冲突，可安全合并' }}
        </span>
      </div>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleMerge" :loading="merging">合并</el-button>
      </template>
    </el-dialog>

    <!-- Git状态对话框 -->
    <el-dialog v-model="gitStatusDialogVisible" title="Git 状态" width="600px">
      <div v-if="gitStatus" class="git-status-detail">
        <div class="status-row">
          <span class="label">分支:</span>
          <el-tag>{{ gitStatus.branchName }}</el-tag>
        </div>
        <div class="status-row">
          <span class="label">当前提交:</span>
          <code>{{ gitStatus.currentCommit?.substring(0, 8) }}</code>
        </div>
        <div class="status-row">
          <span class="label">领先/落后:</span>
          <span>↑{{ gitStatus.aheadCount }} ↓{{ gitStatus.behindCount }}</span>
        </div>
        <div class="status-row">
          <span class="label">修改文件:</span>
          <span>{{ gitStatus.modifiedFiles }}个</span>
        </div>
        <div class="status-row">
          <span class="label">暂存文件:</span>
          <span>{{ gitStatus.stagedFiles }}个</span>
        </div>
        <div class="status-row">
          <span class="label">未跟踪文件:</span>
          <span>{{ gitStatus.untrackedFiles }}个</span>
        </div>
        <div v-if="gitStatus.modifiedFileList?.length" class="file-list">
          <h4>修改的文件:</h4>
          <ul>
            <li v-for="f in gitStatus.modifiedFileList" :key="f">{{ f }}</li>
          </ul>
        </div>
        <div v-if="gitStatus.untrackedFileList?.length" class="file-list">
          <h4>未跟踪的文件:</h4>
          <ul>
            <li v-for="f in gitStatus.untrackedFileList" :key="f">{{ f }}</li>
          </ul>
        </div>
      </div>
      <div v-else v-loading="loadingGitStatus">加载中...</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listWorkspaces,
  createWorkspace,
  removeWorkspace,
  getWorkspaceGitStatus,
  commitChanges,
  pushBranch,
  mergeBranch,
  checkConflicts,
  syncFromBranch,
  type WorkspaceInfo,
  type WorkspaceGitStatus,
  type ConflictCheckResult
} from '@/api/workspace'

// State
const loading = ref(false)
const workspaces = ref<WorkspaceInfo[]>([])
const projectPathFilter = ref('')
const statusFilter = ref('')

// Computed
const filteredWorkspaces = computed(() => {
  return workspaces.value.filter(ws => {
    if (statusFilter.value && ws.status !== statusFilter.value) return false
    if (projectPathFilter.value && !ws.projectPath.includes(projectPathFilter.value)) return false
    return true
  })
})

// Load
async function loadWorkspaces() {
  loading.value = true
  try {
    const res = await listWorkspaces(projectPathFilter.value || undefined)
    workspaces.value = (res as any)?.data || res || []
  } catch (e: any) {
    ElMessage.error('加载工作空间列表失败: ' + (e.message || e))
  } finally {
    loading.value = false
  }
}

// Create
const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = ref({
  projectPath: '',
  branchName: '',
  baseBranch: 'main',
  taskId: '',
  agentId: '',
  description: ''
})

function openCreateDialog() {
  createForm.value = { projectPath: '', branchName: '', baseBranch: 'main', taskId: '', agentId: '', description: '' }
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.value.projectPath) {
    ElMessage.warning('请输入项目路径')
    return
  }
  creating.value = true
  try {
    await createWorkspace({
      projectPath: createForm.value.projectPath,
      branchName: createForm.value.branchName || undefined,
      baseBranch: createForm.value.baseBranch || 'main',
      taskId: createForm.value.taskId || undefined,
      agentId: createForm.value.agentId || undefined,
      description: createForm.value.description || undefined
    })
    ElMessage.success('工作空间创建成功')
    createDialogVisible.value = false
    await loadWorkspaces()
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e.message || e))
  } finally {
    creating.value = false
  }
}

// Git Status
const gitStatusDialogVisible = ref(false)
const loadingGitStatus = ref(false)
const gitStatus = ref<WorkspaceGitStatus | null>(null)

async function viewGitStatus(workspaceId: string) {
  gitStatus.value = null
  gitStatusDialogVisible.value = true
  loadingGitStatus.value = true
  try {
    const res = await getWorkspaceGitStatus(workspaceId)
    gitStatus.value = (res as any)?.data || res
  } catch (e: any) {
    ElMessage.error('获取Git状态失败: ' + (e.message || e))
  } finally {
    loadingGitStatus.value = false
  }
}

// Commit
const commitDialogVisible = ref(false)
const committing = ref(false)
const commitForm = ref({ message: '' })
const currentCommitWorkspaceId = ref('')

function openCommitDialog(workspaceId: string) {
  currentCommitWorkspaceId.value = workspaceId
  commitForm.value.message = ''
  commitDialogVisible.value = true
}

async function handleCommit() {
  if (!commitForm.value.message) {
    ElMessage.warning('请输入提交信息')
    return
  }
  committing.value = true
  try {
    const res = await commitChanges(currentCommitWorkspaceId.value, commitForm.value.message)
    const result = (res as any)?.data || res
    if (result?.success) {
      ElMessage.success('提交成功: ' + (result.commitHash?.substring(0, 8) || ''))
    } else {
      ElMessage.warning(result?.error || result?.message || '提交失败')
    }
    commitDialogVisible.value = false
    await loadWorkspaces()
  } catch (e: any) {
    ElMessage.error('提交失败: ' + (e.message || e))
  } finally {
    committing.value = false
  }
}

// Push
async function handlePush(workspaceId: string) {
  try {
    const res = await pushBranch(workspaceId)
    const result = (res as any)?.data || res
    if (result?.success) {
      ElMessage.success('推送成功')
    } else {
      ElMessage.error('推送失败: ' + (result?.error || ''))
    }
  } catch (e: any) {
    ElMessage.error('推送失败: ' + (e.message || e))
  }
}

// Merge
const mergeDialogVisible = ref(false)
const merging = ref(false)
const checkingConflicts = ref(false)
const mergeForm = ref({ targetBranch: 'main' })
const currentMergeWorkspaceId = ref('')
const conflictResult = ref<ConflictCheckResult | null>(null)

function openMergeDialog(workspaceId: string) {
  currentMergeWorkspaceId.value = workspaceId
  mergeForm.value.targetBranch = 'main'
  conflictResult.value = null
  mergeDialogVisible.value = true
}

async function handleCheckConflicts() {
  checkingConflicts.value = true
  try {
    const res = await checkConflicts(currentMergeWorkspaceId.value, mergeForm.value.targetBranch)
    conflictResult.value = (res as any)?.data || res
  } catch (e: any) {
    ElMessage.error('冲突检测失败: ' + (e.message || e))
  } finally {
    checkingConflicts.value = false
  }
}

async function handleMerge() {
  merging.value = true
  try {
    const res = await mergeBranch(currentMergeWorkspaceId.value, mergeForm.value.targetBranch)
    const result = (res as any)?.data || res
    if (result?.success) {
      ElMessage.success('合并成功')
      mergeDialogVisible.value = false
      await loadWorkspaces()
    } else if (result?.hasConflicts) {
      ElMessage.error('合并存在冲突，请先解决冲突')
    } else {
      ElMessage.error('合并失败: ' + (result?.error || ''))
    }
  } catch (e: any) {
    ElMessage.error('合并失败: ' + (e.message || e))
  } finally {
    merging.value = false
  }
}

// Sync
async function handleSync(workspaceId: string, baseBranch: string) {
  try {
    const res = await syncFromBranch(workspaceId, baseBranch)
    const result = (res as any)?.data || res
    if (result?.success) {
      ElMessage.success('同步成功 (策略: ' + result.strategy + ')')
    } else if (result?.hasConflicts) {
      ElMessage.error('同步存在冲突: ' + (result.conflictFiles?.join(', ') || ''))
    } else {
      ElMessage.error('同步失败: ' + (result?.error || ''))
    }
  } catch (e: any) {
    ElMessage.error('同步失败: ' + (e.message || e))
  }
}

// Remove
async function handleRemove(workspaceId: string) {
  try {
    await ElMessageBox.confirm('确定要删除该工作空间？将移除Worktree和对应分支。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeWorkspace(workspaceId, false)
    ElMessage.success('工作空间已删除')
    await loadWorkspaces()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败: ' + (e.message || e))
    }
  }
}

// Helpers
function getStatusClass(status: string) {
  const map: Record<string, string> = {
    'ACTIVE': 'status-active',
    'COMMITTED': 'status-committed',
    'MERGED': 'status-merged',
    'REMOVED': 'status-removed',
    'ERROR': 'status-error'
  }
  return map[status] || ''
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    'ACTIVE': '活跃',
    'COMMITTED': '已提交',
    'MERGED': '已合并',
    'REMOVED': '已删除',
    'ERROR': '错误'
  }
  return map[status] || status
}

function formatTime(time: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

// Init
onMounted(() => {
  loadWorkspaces()
})
</script>

<style scoped>
.workspace-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 8px;
}

.page-title {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.page-subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.workspace-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.workspace-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.3s;
}

.workspace-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.workspace-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.workspace-icon {
  font-size: 28px;
}

.workspace-info {
  flex: 1;
}

.workspace-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.workspace-desc {
  font-size: 13px;
  color: #909399;
  margin: 4px 0 0 0;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active { background: #e1f3d8; color: #67c23a; }
.status-committed { background: #d9ecff; color: #409eff; }
.status-merged { background: #e6a23c20; color: #e6a23c; }
.status-removed { background: #f5f5f5; color: #909399; }
.status-error { background: #fde2e2; color: #f56c6c; }

.workspace-meta {
  margin: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item {
  font-size: 13px;
  color: #606266;
}

.meta-label {
  font-weight: 500;
  margin-right: 8px;
  color: #909399;
}

.meta-value {
  word-break: break-all;
}

.commit-hash {
  font-family: monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
}

.workspace-actions {
  margin: 12px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workspace-time {
  font-size: 12px;
  color: #c0c4cc;
}

.git-status-detail .status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.git-status-detail .label {
  font-weight: 500;
  color: #606266;
  width: 80px;
}

.file-list {
  margin-top: 12px;
}

.file-list h4 {
  color: #606266;
  margin-bottom: 4px;
}

.file-list ul {
  padding-left: 20px;
}

.file-list li {
  font-family: monospace;
  font-size: 13px;
  color: #909399;
}
</style>
