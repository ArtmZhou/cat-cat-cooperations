<template>
  <div class="chat-sidebar">
    <div class="sidebar-header">
      <h3>群聊</h3>
      <el-button size="small" type="primary" @click="$emit('create')">
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>

    <div class="group-list">
      <div
        v-for="group in groups"
        :key="group.id"
        :class="['group-item', { active: group.id === selectedId }]"
        @click="$emit('select', group)"
      >
        <div class="group-avatar">👥</div>
        <div class="group-info">
          <div class="group-name">{{ group.name }}</div>
          <div class="group-meta">{{ group.agents?.length || 0 }} 个Agent</div>
        </div>
        <el-dropdown @command="(cmd: string) => $emit('action', cmd, group)" trigger="click" @click.stop>
          <el-button text size="small">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="edit">编辑</el-dropdown-item>
              <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-empty v-if="groups.length === 0" description="暂无群组，请创建" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, MoreFilled } from '@element-plus/icons-vue'
import type { ChatGroup } from '@/types/models'

defineProps<{
  groups: ChatGroup[]
  selectedId?: string
}>()

defineEmits<{
  select: [group: ChatGroup]
  create: []
  action: [command: string, group: ChatGroup]
}>()
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.chat-sidebar {
  width: 280px;
  background: $bg-deep;
  border-right: 1px solid $border-subtle;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  justify-content: space-between;
  align-items: center;

  h3 {
    margin: 0;
    font-size: 16px;
    color: $text-primary;
  }
}

.group-list {
  flex: 1;
  overflow-y: auto;
}

.group-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(124, 58, 237, 0.05);
  transition: background 0.2s;
  position: relative;

  &:hover {
    background: $bg-hover;
  }

  &.active {
    background: $bg-surface;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 8px;
      bottom: 8px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: linear-gradient(180deg, $color-violet, $color-cyan);
    }
  }
}

.group-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-name {
  font-weight: 600;
  font-size: 14px;
  color: $text-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.group-meta {
  font-size: 12px;
  color: $text-muted;
}
</style>
