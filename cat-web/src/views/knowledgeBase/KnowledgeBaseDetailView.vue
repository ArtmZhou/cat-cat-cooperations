<template>
  <div class="kb-detail-page">
    <div class="page-toolbar">
      <el-button link type="primary" @click="$router.push('/knowledge-bases')">
        <el-icon><ArrowLeft /></el-icon>
        返回知识库列表
      </el-button>
    </div>

    <div class="detail-card" v-if="kb">
      <h2 class="kb-title">{{ kb.name }}</h2>
      <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
      <div class="kb-meta">
        <el-tag size="small">分块: {{ kb.chunkSize }}</el-tag>
        <el-tag size="small" type="success">Token预算: {{ kb.maxTokens }}</el-tag>
        <el-tag size="small" type="info">{{ kb.embeddingProvider || '嵌入未配置' }}</el-tag>
      </div>

      <el-divider />

      <div class="section">
        <div class="section-header">
          <h4>文档列表 ({{ documents.length }})</h4>
          <el-upload
            :action="uploadUrl"
            :headers="{}"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            accept=".md,.txt,.pdf,.java,.ts,.vue,.js,.py,.xml,.yaml,.yml,.json"
          >
            <el-button type="primary" size="small">
              <el-icon><Upload /></el-icon>
              上传文档
            </el-button>
          </el-upload>
        </div>

        <el-table :data="documents" v-loading="loadingDocs" class="glass-table" empty-text="暂无文档">
          <el-table-column prop="fileName" label="文件名" min-width="200" />
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-progress
                v-if="row.status === 'EMBEDDING'"
                :percentage="row.chunkCount ? Math.round(row.embeddedChunks / row.chunkCount * 100) : 0"
                :stroke-width="6"
                style="width: 100px"
              />
              <el-tag v-else :type="row.status === 'READY' ? 'success' : 'info'" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="100">
            <template #default="{ row }">
              {{ row.embeddedChunks }}/{{ row.chunkCount }}
            </template>
          </el-table-column>
          <el-table-column label="大小" width="90">
            <template #default="{ row }">
              {{ formatSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="deleteDoc(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-divider />

      <div class="section">
        <h4>检索测试</h4>
        <div class="search-box">
          <el-input v-model="searchQuery" placeholder="输入查询文本..." @keyup.enter="doSearch">
            <template #append>
              <el-button @click="doSearch" :loading="searching">搜索</el-button>
            </template>
          </el-input>
        </div>

        <div v-if="searchResults.length > 0" class="search-results">
          <div v-for="(r, i) in searchResults" :key="i" class="result-item">
            <div class="result-header">
              <el-tag size="small" type="info">{{ r.metadata?.fileName || '-' }}</el-tag>
              <span class="result-score">相似度: {{ (r.score * 100).toFixed(1) }}%</span>
            </div>
            <p class="result-content">{{ r.content?.substring(0, 300) }}{{ r.content?.length > 300 ? '...' : '' }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { getKnowledgeBase, type KnowledgeBase } from '@/api/knowledgeBase'
import request from '@/utils/request'
import type { Client } from '@stomp/stompjs'
import { Client as StompClient } from '@stomp/stompjs'

interface Document {
  id: string
  kbId: string
  fileName: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  embeddedChunks: number
  createdAt: string
}

interface SearchResult {
  chunkId: string
  docId: string
  kbId: string
  content: string
  metadata: Record<string, string>
  score: number
}

const route = useRoute()
const kbId = computed(() => route.params.kbId as string)
const uploadUrl = computed(() => `/api/v1/knowledge-bases/${kbId.value}/documents`)

const kb = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const loadingDocs = ref(false)
const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref<SearchResult[]>([])
let stompClient: Client | null = null

function statusText(s: string): string {
  const map: Record<string, string> = { PENDING: '待处理', CHUNKING: '分块中', EMBEDDING: '嵌入中', READY: '就绪', ERROR: '失败' }
  return map[s] || s
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function beforeUpload(file: File): boolean {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.warning('文件大小超过10MB限制')
    return false
  }
  return true
}

function onUploadSuccess() {
  ElMessage.success('上传成功，正在嵌入...')
  fetchDocuments()
}

function onUploadError() {
  ElMessage.error('上传失败')
}

async function fetchDetail() {
  kb.value = await getKnowledgeBase(kbId.value)
}

async function fetchDocuments() {
  loadingDocs.value = true
  try {
    documents.value = await request.get(`/knowledge-bases/${kbId.value}/documents`)
  } finally {
    loadingDocs.value = false
  }
}

async function deleteDoc(doc: Document) {
  try {
    await ElMessageBox.confirm(`删除文档「${doc.fileName}」？`, '确认删除', { type: 'warning' })
  } catch { return }
  await request.delete(`/knowledge-bases/${kbId.value}/documents/${doc.id}`)
  ElMessage.success('已删除')
  fetchDocuments()
}

async function doSearch() {
  if (!searchQuery.value.trim()) return
  searching.value = true
  try {
    const res = await request.post(`/knowledge-bases/${kbId.value}/search`, {
      query: searchQuery.value,
      topK: 5
    })
    searchResults.value = res as SearchResult[]
  } finally {
    searching.value = false
  }
}

function connectWebSocket() {
  const wsUrl = new URL('/ws', window.location.href.replace('http', 'ws'))
  stompClient = new StompClient({ brokerURL: wsUrl.href })
  stompClient.onConnect = () => {
    stompClient!.subscribe(`/topic/kb/${kbId.value}/embedding-progress`, (msg) => {
      const progress = JSON.parse(msg.body)
      const idx = documents.value.findIndex(d => d.id === progress.docId)
      if (idx >= 0) {
        documents.value[idx].embeddedChunks = progress.embedded
        documents.value[idx].status = progress.status
      }
    })
  }
  stompClient.activate()
}

onMounted(async () => {
  await fetchDetail()
  await fetchDocuments()
  connectWebSocket()
})

onUnmounted(() => {
  stompClient?.deactivate()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.kb-detail-page { max-width: 1000px; }

.page-toolbar { margin-bottom: $space-md; }

.detail-card {
  background: $bg-surface;
  border: 1px solid $border-subtle;
  border-radius: $radius-lg;
  padding: $space-lg;
}

.kb-title { font-size: 22px; font-weight: 700; color: $text-primary; margin: 0 0 8px; }
.kb-desc { color: $text-secondary; font-size: 14px; margin: 0 0 12px; }
.kb-meta { display: flex; gap: 8px; }

.section {
  margin-bottom: $space-lg;
  h4 { font-size: 15px; color: $text-primary; margin: 0 0 12px; }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  h4 { margin: 0; }
}

.search-box {
  width: 100%;
  max-width: 600px;
}

.search-results {
  margin-top: $space-md;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  background: $bg-base;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  padding: 12px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.result-score {
  font-size: 12px;
  color: $color-cyan;
  font-family: monospace;
}

.result-content {
  font-size: 13px;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}
</style>
