<template>
  <div class="mention-popup">
    <div class="mention-header">
      {{ mode === 'filter' ? '输入名称筛选Agent：' : '选择要@的Agent：' }}
    </div>
    <div
      v-for="(agent, idx) in agents"
      :key="agent.id"
      :class="['mention-item', {
        selected: mentionedAgentIds.includes(agent.id),
        highlighted: mode === 'filter' && idx === highlightIndex
      }]"
      @click="mode === 'filter' ? $emit('selectFromFilter', agent.id) : $emit('toggle', agent.id)"
    >
      <span class="mention-avatar">🤖</span>
      <span class="mention-name">{{ agent.name }}</span>
      <el-tag size="small" :type="getAgentTagType(agent.status)">
        {{ getAgentStatusText(agent.status) }}
      </el-tag>
      <el-icon v-if="mentionedAgentIds.includes(agent.id)" class="mention-check"><Check /></el-icon>
    </div>
    <div v-if="agents.length === 0 && mode === 'filter'" class="mention-empty">
      无匹配的Agent
    </div>
  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import type { AgentBrief } from '@/types/models'

defineProps<{
  agents: AgentBrief[]
  mentionedAgentIds: string[]
  highlightIndex: number
  mode: 'select' | 'filter'
}>()

defineEmits<{
  toggle: [agentId: string]
  selectFromFilter: [agentId: string]
  close: []
}>()

function getAgentTagType(status: string): string {
  const map: Record<string, string> = {
    RUNNING: 'success',
    EXECUTING: 'primary',
    STOPPED: 'info',
    ERROR: 'danger'
  }
  return map[status] || 'info'
}

function getAgentStatusText(status: string): string {
  const map: Record<string, string> = {
    RUNNING: '运行中',
    EXECUTING: '执行中',
    STOPPED: '已停止',
    ERROR: '错误'
  }
  return map[status] || status
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.mention-popup {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: $bg-elevated;
  border: 1px solid $border-active;
  border-radius: $radius-md;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.3);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.mention-header {
  padding: 8px 12px;
  font-size: 12px;
  color: $text-muted;
  border-bottom: 1px solid $border-subtle;
}

.mention-item {
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background 0.2s;
  color: $text-secondary;

  &:hover {
    background: $bg-hover;
  }

  &.selected {
    background: $color-violet-dim;
  }

  &.highlighted {
    background: $color-violet-dim;
    outline: 1px solid $color-violet;
    outline-offset: -1px;
  }
}

.mention-empty {
  padding: 12px;
  text-align: center;
  color: $text-muted;
  font-size: 13px;
}

.mention-avatar {
  font-size: 16px;
}

.mention-name {
  flex: 1;
  font-size: 13px;
  color: $text-primary;
}

.mention-check {
  color: $color-violet;
}
</style>
