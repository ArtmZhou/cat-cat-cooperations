<template>
  <div class="dashboard">
    <h1 class="page-title">仪表盘</h1>

    <div class="stats-grid" v-loading="loading">
      <div class="stat-card">
        <div class="stat-icon">🤖</div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.totalAgents }}</span>
          <span class="stat-label">CLI Agent数量</span>
          <span class="stat-sub">运行中: {{ overview.runningAgents }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">⚡</div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.executingAgents }}</span>
          <span class="stat-label">执行中Agent</span>
          <span class="stat-sub">活跃会话: {{ overview.activeSessions }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <span class="stat-value">{{ formatTokens(overview.totalInputTokens + overview.totalOutputTokens) }}</span>
          <span class="stat-label">Token使用总量</span>
          <span class="stat-sub">输入: {{ formatTokens(overview.totalInputTokens) }} / 输出: {{ formatTokens(overview.totalOutputTokens) }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🔄</div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.concurrentTasks }}</span>
          <span class="stat-label">并发任务数</span>
          <span class="stat-sub">错误: {{ overview.errorAgents }}</span>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h2 class="section-title">快速操作</h2>
      <div class="action-grid">
        <div class="action-card" @click="$router.push('/cli-agents')">
          <div class="action-icon">➕</div>
          <div class="action-info">
            <span class="action-title">创建CLI Agent</span>
            <span class="action-desc">基于模板快速创建Agent实例</span>
          </div>
        </div>
        <div class="action-card" @click="$router.push('/group-chat')">
          <div class="action-icon">💬</div>
          <div class="action-info">
            <span class="action-title">群聊协作</span>
            <span class="action-desc">多Agent群聊协同工作</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getSystemOverview } from '@/api/cliAgent'

const loading = ref(false)
const overview = ref({
  totalAgents: 0,
  runningAgents: 0,
  executingAgents: 0,
  stoppedAgents: 0,
  errorAgents: 0,
  totalInputTokens: 0,
  totalOutputTokens: 0,
  activeSessions: 0,
  concurrentTasks: 0
})

let refreshTimer: any = null

onMounted(() => {
  loadOverview()
  // 每30秒刷新一次
  refreshTimer = setInterval(loadOverview, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})

async function loadOverview() {
  loading.value = true
  try {
    const data = await getSystemOverview()
    overview.value = data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

function formatTokens(tokens: number): string {
  if (!tokens) return '0'
  if (tokens >= 1000000) {
    return (tokens / 1000000).toFixed(1) + 'M'
  }
  if (tokens >= 1000) {
    return (tokens / 1000).toFixed(1) + 'K'
  }
  return tokens.toString()
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}
.page-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
  border: 1px solid #F0E6D8;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 115, 85, 0.12);
}
.stat-icon {
  width: 48px;
  height: 48px;
  background: #FFF5E6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #FF8C42;
}
.stat-label {
  font-size: 13px;
  color: #8C8C8C;
}
.stat-sub {
  font-size: 12px;
  color: #BFBFBF;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.action-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(139, 115, 85, 0.08);
  border: 1px solid #F0E6D8;
  cursor: pointer;
  transition: all 0.2s;
}
.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 115, 85, 0.12);
  border-color: #FF8C42;
}
.action-icon {
  width: 40px;
  height: 40px;
  background: #FFF5E6;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.action-info {
  display: flex;
  flex-direction: column;
}
.action-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}
.action-desc {
  font-size: 12px;
  color: #8C8C8C;
}
</style>