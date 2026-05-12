<template>
  <div class="app-layout">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <CatLogo :size="28" />
        <span v-show="!isCollapsed" class="logo-text">smart cats</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isMenuActive(item.path) }"
        >
          <component :is="item.icon" :size="20" />
          <span v-show="!isCollapsed" class="nav-label">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="main-container">
      <header class="header">
        <div class="header-left">
          <button class="toggle-btn" @click="isCollapsed = !isCollapsed">
            <FoldIcon v-if="!isCollapsed" :size="18" />
            <ExpandIcon v-else :size="18" />
          </button>
        </div>
        <div class="header-right">
          <span class="avatar-ring">
            <el-avatar :size="30" class="user-avatar">U</el-avatar>
          </span>
        </div>
      </header>

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  CatLogo,
  DashboardIcon,
  TerminalIcon,
  MessageBubbleIcon,
  BookIcon,
  FoldIcon,
  ExpandIcon
} from '@/components/CatIcons'

const route = useRoute()

const isCollapsed = ref(false)

const menuItems = [
  { path: '/dashboard', label: '仪表盘', icon: DashboardIcon },
  { path: '/cli-agents', label: 'CLI Agent', icon: TerminalIcon },
  { path: '/group-chat', label: '群聊', icon: MessageBubbleIcon },
  { path: '/knowledge-bases', label: '知识库', icon: BookIcon }
]

function isMenuActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}

</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.app-layout {
  display: flex;
  width: 100%;
  height: 100%;
}

// ===== Sidebar =====
.sidebar {
  width: 220px;
  background: $bg-deep;
  border-right: 1px solid $border-subtle;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  &.collapsed {
    width: 64px;
    .sidebar-header { justify-content: center; }
    .nav-item { justify-content: center; padding: 0 12px; }
  }
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid $border-subtle;

  .logo-text {
    font-size: 17px;
    font-weight: 700;
    background: linear-gradient(135deg, $color-violet, $color-cyan);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    white-space: nowrap;
  }
}

.sidebar-nav {
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: $radius-md;
  color: $text-secondary;
  text-decoration: none;
  transition: all 0.2s ease;
  position: relative;
  cursor: pointer;

  &:hover {
    background: $bg-hover;
    color: $text-primary;
  }

  &.active {
    background: rgba(124, 58, 237, 0.08);
    color: $text-primary;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 6px;
      bottom: 6px;
      width: 3px;
      border-radius: 0 3px 3px 0;
      background: linear-gradient(180deg, $color-violet, $color-cyan);
    }

    span:first-of-type, :deep(svg) {
      color: $color-violet;
    }
  }

  .nav-label {
    font-size: 14px;
    white-space: nowrap;
  }
}

// ===== Header =====
.header {
  height: 64px;
  background: rgba(15, 17, 23, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid $border-subtle;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.toggle-btn {
  background: none;
  border: none;
  color: $text-secondary;
  cursor: pointer;
  padding: 6px;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  transition: all 0.2s;

  &:hover {
    color: $text-primary;
    background: $bg-hover;
  }
}

.avatar-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border-radius: 50%;
  background: linear-gradient(135deg, $color-violet, $color-cyan);
}

.user-avatar {
  background: $bg-surface !important;
  color: $text-primary !important;
  font-weight: 600;
  font-size: 13px;
}

// ===== Main Content =====
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: $bg-base;
}
</style>