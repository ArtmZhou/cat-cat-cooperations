<template>
  <div class="dashboard">
    <h1 class="page-title gradient-text">仪表盘</h1>

    <div class="stats-grid" v-loading="loading">
      <div class="stat-card" v-for="stat in statCards" :key="stat.label">
        <div class="stat-icon-wrapper">
          <component :is="stat.icon" :size="22" />
        </div>
        <div class="stat-info">
          <span class="stat-value gradient-text">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
          <span class="stat-sub">{{ stat.sub }}</span>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <h2 class="section-title">快速操作</h2>
      <div class="action-grid">
        <div class="action-card" @click="$router.push('/cli-agents')">
          <div class="action-icon-wrapper">
            <PlusCircleIcon :size="20" />
          </div>
          <div class="action-info">
            <span class="action-title">创建CLI Agent</span>
            <span class="action-desc">基于模板快速创建Agent实例</span>
          </div>
        </div>
        <div class="action-card" @click="$router.push('/group-chat')">
          <div class="action-icon-wrapper">
            <MessageBubbleIcon :size="20" />
          </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getSystemOverview } from '@/api/cliAgent'
import {
  RobotAgent,
  LightningIcon,
  ChartBarIcon,
  RefreshIcon,
  PlusCircleIcon,
  MessageBubbleIcon
} from '@/components/CatIcons.vue'

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
  refreshTimer = setInterval(loadOverview, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})

const statCards = computed(() => [
  {
    icon: RobotAgent,
    value: overview.value.totalAgents,
    label: 'CLI Agent数量',
    sub: `运行中: ${overview.value.runningAgents}`
  },
  {
    icon: LightningIcon,
    value: overview.value.executingAgents,
    label: '执行中Agent',
    sub: `活跃会话: ${overview.value.activeSessions}`
  },
  {
    icon: ChartBarIcon,
    value: formatTokens(overview.value.totalInputTokens + overview.value.totalOutputTokens),
    label: 'Token使用总量',
    sub: `输入: ${formatTokens(overview.value.totalInputTokens)} / 输出: ${formatTokens(overview.value.totalOutputTokens)}`
  },
  {
    icon: RefreshIcon,
    value: overview.value.concurrentTasks,
    label: '并发任务数',
    sub: `错误: ${overview.value.errorAgents}`
  }
])

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
  if (tokens >= 1000000) return (tokens / 1000000).toFixed(1) + 'M'
  if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'K'
  return tokens.toString()
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.dashboard {
  padding: 0;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 28px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 36px;
}

.stat-card {
  background: $bg-surface;
  border-radius: $radius-md;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid $border-subtle;
  cursor: default;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: $border-active;
    box-shadow: $glow-violet;
  }
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-violet;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-top: 2px;
}

.stat-sub {
  font-size: 12px;
  color: $text-muted;
  margin-top: 2px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: $text-primary;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-card {
  background: $bg-surface;
  border-radius: $radius-md;
  padding: 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid $border-subtle;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: $border-active;
    box-shadow: $glow-violet;
  }
}

.action-icon-wrapper {
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $color-violet;
  flex-shrink: 0;
}

.action-info {
  display: flex;
  flex-direction: column;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.action-desc {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}
</style>