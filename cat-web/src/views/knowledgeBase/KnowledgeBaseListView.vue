<template>
  <div class="kb-list-page">
    <div class="page-toolbar">
      <h2 class="page-title">知识库</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建知识库
      </el-button>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      class="glass-table"
      empty-text="暂无知识库，点击上方按钮创建一个"
    >
      <el-table-column prop="name" label="名称" min-width="180">
        <template #default="{ row }">
          <el-button link type="primary" class="kb-name" @click="router.push(`/knowledge-bases/${row.id}`)">
            {{ row.name }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="嵌入服务" width="160">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.embeddingProvider || '未配置' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="向量库" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.vectorStoreProvider || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分块/Token" width="160">
        <template #default="{ row }">
          <span class="cfg-text">{{ row.chunkSize }} / {{ row.maxTokens }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑知识库' : '新建知识库'"
      width="540px"
      :close-on-click-modal="false"
      class="glass-dialog"
    >
      <el-form :model="form" label-position="top" ref="formRef">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如：前端规范、API设计" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="知识库用途说明" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="嵌入 Provider">
              <el-input v-model="form.embeddingProvider" placeholder="openai" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="嵌入模型">
              <el-input v-model="form.embeddingModel" placeholder="text-embedding-3-small" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="向量库 Provider">
              <el-input v-model="form.vectorStoreProvider" placeholder="lucene" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="分块大小">
              <el-input-number v-model="form.chunkSize" :min="128" :max="4096" :step="64" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="重叠">
              <el-input-number v-model="form.chunkOverlap" :min="0" :max="512" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="Token 预算上限">
          <el-input-number v-model="form.maxTokens" :min="256" :max="8192" :step="256" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ editing ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getKnowledgeBases,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  type KnowledgeBase,
  type KnowledgeBaseForm
} from '@/api/knowledgeBase'

const router = useRouter()
const loading = ref(false)
const tableData = ref<KnowledgeBase[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

const dialogVisible = ref(false)
const editing = ref<KnowledgeBase | null>(null)
const submitting = ref(false)
const form = ref<KnowledgeBaseForm>(defaultForm())

function defaultForm(): KnowledgeBaseForm {
  return {
    name: '',
    description: '',
    embeddingProvider: '',
    embeddingModel: '',
    vectorStoreProvider: 'lucene',
    chunkSize: 512,
    chunkOverlap: 64,
    maxTokens: 2000
  }
}

function formatTime(t: string): string {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getKnowledgeBases(currentPage.value, pageSize)
    tableData.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editing.value = null
  form.value = defaultForm()
  dialogVisible.value = true
}

function openEditDialog(row: KnowledgeBase) {
  editing.value = row
  form.value = {
    name: row.name,
    description: row.description,
    embeddingProvider: row.embeddingProvider,
    embeddingModel: row.embeddingModel,
    vectorStoreProvider: row.vectorStoreProvider,
    chunkSize: row.chunkSize,
    chunkOverlap: row.chunkOverlap,
    maxTokens: row.maxTokens
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  submitting.value = true
  try {
    if (editing.value) {
      await updateKnowledgeBase(editing.value.id, form.value)
      ElMessage.success('知识库已更新')
    } else {
      await createKnowledgeBase(form.value)
      ElMessage.success('知识库已创建')
    }
    dialogVisible.value = false
    await fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: KnowledgeBase) {
  try {
    await ElMessageBox.confirm(
      `删除知识库「${row.name}」将同时清理其下所有文档和分块数据，确定继续？`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteKnowledgeBase(row.id)
  ElMessage.success('已删除')
  await fetchList()
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.kb-list-page {
  max-width: 1200px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $space-lg;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
  margin: 0;
}

.kb-name {
  font-weight: 600;
  color: $text-primary;
}

.cfg-text {
  font-size: 13px;
  color: $text-secondary;
  font-family: monospace;
}

.pagination-wrap {
  margin-top: $space-lg;
  display: flex;
  justify-content: flex-end;
}
</style>
