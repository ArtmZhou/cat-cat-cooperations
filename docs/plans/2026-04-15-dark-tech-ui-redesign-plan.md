# Dark Tech UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the warm-orange light UI into a dark tech theme with purple-blue gradient (#7C3AED → #06B6D4) and refined cat SVG icons.

**Architecture:** Replace SCSS variables and global styles for the dark design system, override Element Plus theme via CSS custom properties, create inline SVG Vue icon components, then restyle each page view (AppLayout → Dashboard → CliAgentList → GroupChat) with dark surfaces, gradient accents, glow effects, and status animations.

**Tech Stack:** Vue 3 + TypeScript, SCSS + CSS Custom Properties, Element Plus (theme override only), inline SVG components

---

## File Structure

| File | Action | Responsibility |
|------|--------|---------------|
| `cat-web/src/assets/styles/_variables.scss` | Replace | Dark design system tokens (colors, borders, shadows, animations) |
| `cat-web/src/assets/styles/main.scss` | Replace | Global reset, base styles, Element Plus dark overrides, utility classes |
| `cat-web/src/components/icons/CatIcons.vue` | Create | All SVG icon components in a single file (CatLogo, RobotAgent, etc.) |
| `cat-web/src/components/layout/AppLayout.vue` | Modify | Dark sidebar, glassmorphism header, custom nav menu, gradient accents |
| `cat-web/src/views/dashboard/DashboardView.vue` | Modify | Dark stat cards, gradient values, SVG icons, glow hover effects |
| `cat-web/src/views/cliAgent/CliAgentListView.vue` | Modify | Dark agent cards, status animations, gradient buttons, dark filter bar |
| `cat-web/src/views/groupChat/GroupChatView.vue` | Modify | Dark chat layout, gradient message bubbles, dark mention popup |

---

### Task 1: Design System Foundation

**Files:**
- Replace: `cat-web/src/assets/styles/_variables.scss`
- Replace: `cat-web/src/assets/styles/main.scss`

- [ ] **Step 1: Replace _variables.scss with dark design system**

Replace the entire content of `cat-web/src/assets/styles/_variables.scss` with:

```scss
// ============================================
// 猫猫Agent协同系统 — 深色科技风设计系统
// ============================================

// 背景层级
$bg-deep:     #0B0D14;
$bg-base:     #0F1117;
$bg-surface:  #161823;
$bg-elevated: #1C1F2E;
$bg-hover:    #252836;

// 主色
$color-violet:     #7C3AED;
$color-cyan:       #06B6D4;
$color-violet-dim: rgba(124, 58, 237, 0.15);
$color-cyan-dim:   rgba(6, 182, 212, 0.15);

// 文字层级
$text-primary:   #F0F0F0;
$text-secondary: #9CA3AF;
$text-muted:     #4B5563;

// 语义色
$status-running:   #06B6D4;
$status-executing: #7C3AED;
$status-stopped:   #4B5563;
$status-error:     #EF4444;
$success:          #10B981;
$warning:          #F59E0B;
$danger:           #EF4444;

// 边框
$border-subtle:  rgba(124, 58, 237, 0.12);
$border-active:  rgba(124, 58, 237, 0.4);

// 阴影 & 发光
$glow-violet:    0 0 20px rgba(124, 58, 237, 0.15);
$glow-cyan:      0 0 20px rgba(6, 182, 212, 0.15);
$glow-strong:    0 0 30px rgba(124, 58, 237, 0.25), 0 0 60px rgba(6, 182, 212, 0.1);

// 间距
$space-xs: 4px;
$space-sm: 8px;
$space-md: 16px;
$space-lg: 24px;
$space-xl: 32px;

// 圆角
$radius-sm: 6px;
$radius-md: 10px;
$radius-lg: 14px;
$radius-xl: 20px;
```

- [ ] **Step 2: Replace main.scss with dark global styles + Element Plus overrides**

Replace the entire content of `cat-web/src/assets/styles/main.scss` with:

```scss
@use './variables' as *;

// ============================================
// Global Reset
// ============================================
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: 'Inter', 'PingFang SC', -apple-system, sans-serif;
  font-size: 14px;
  color: $text-primary;
  background-color: $bg-base;
}

// ============================================
// Element Plus Dark Theme Overrides
// ============================================
:root {
  --el-bg-color: #{$bg-surface};
  --el-bg-color-overlay: #{$bg-elevated};
  --el-bg-color-page: #{$bg-base};
  --el-text-color-primary: #{$text-primary};
  --el-text-color-regular: #{$text-secondary};
  --el-text-color-secondary: #{$text-muted};
  --el-text-color-placeholder: #{$text-muted};
  --el-border-color: #{$border-subtle};
  --el-border-color-light: rgba(124, 58, 237, 0.08);
  --el-border-color-lighter: rgba(124, 58, 237, 0.05);
  --el-border-color-dark: #{$border-active};
  --el-fill-color: #{$bg-hover};
  --el-fill-color-light: #{$bg-surface};
  --el-fill-color-lighter: #{$bg-elevated};
  --el-fill-color-blank: #{$bg-surface};
  --el-color-primary: #{$color-violet};
  --el-color-primary-light-3: rgba(124, 58, 237, 0.6);
  --el-color-primary-light-5: rgba(124, 58, 237, 0.4);
  --el-color-primary-light-7: rgba(124, 58, 237, 0.25);
  --el-color-primary-light-8: rgba(124, 58, 237, 0.15);
  --el-color-primary-light-9: rgba(124, 58, 237, 0.08);
  --el-color-primary-dark-2: #6D28D9;
  --el-color-success: #{$success};
  --el-color-warning: #{$warning};
  --el-color-danger: #{$danger};
  --el-color-info: #{$text-muted};
  --el-mask-color: rgba(11, 13, 20, 0.8);
  --el-dialog-bg-color: #{$bg-elevated};
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #{$text-secondary};
  --el-menu-active-color: #{$text-primary};
  --el-menu-hover-bg-color: #{$bg-hover};
  --el-input-bg-color: #{$bg-surface};
  --el-input-border-color: #{$border-subtle};
  --el-input-text-color: #{$text-primary};
  --el-disabled-bg-color: #{$bg-surface};
  --el-disabled-text-color: #{$text-muted};
  --el-disabled-border-color: #{$border-subtle};
  --el-button-bg-color: #{$bg-surface};
  --el-button-text-color: #{$text-primary};
  --el-button-border-color: #{$border-subtle};
  --el-button-hover-bg-color: #{$bg-hover};
  --el-button-hover-text-color: #{$text-primary};
  --el-button-hover-border-color: #{$border-active};
  --el-dropdown-menu-box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  --el-popper-border-color: #{$border-subtle};
  --el-loading-spinner-size: 42px;
  --el-tag-bg-color: #{$bg-hover};
  --el-tag-border-color: #{$border-subtle};
  --el-tag-text-color: #{$text-secondary};
  --el-empty-description-margin-top: 20px;
}

// Element Plus component-level fixes
.el-dialog {
  border: 1px solid $border-subtle;
  border-radius: $radius-lg !important;
}

.el-dialog__header {
  border-bottom: 1px solid $border-subtle;
  padding-bottom: 16px;
}

.el-dialog__title {
  color: $text-primary;
}

.el-divider {
  border-color: $border-subtle;
}

.el-divider__text {
  background-color: $bg-elevated;
  color: $text-secondary;
}

.el-select-dropdown {
  background: $bg-elevated !important;
  border: 1px solid $border-subtle !important;
}

.el-select-dropdown__item {
  color: $text-secondary;
  &:hover, &.hover {
    background: $bg-hover;
  }
  &.selected {
    color: $color-violet;
  }
}

.el-message-box {
  background: $bg-elevated;
  border: 1px solid $border-subtle;
}

.el-loading-mask {
  background-color: rgba(11, 13, 20, 0.6);
}

.el-empty__description p {
  color: $text-muted;
}

.el-tag--info {
  --el-tag-bg-color: #{$bg-hover};
  --el-tag-border-color: #{$border-subtle};
  --el-tag-text-color: #{$text-secondary};
}

.el-tag--success {
  --el-tag-bg-color: rgba(16, 185, 129, 0.1);
  --el-tag-border-color: rgba(16, 185, 129, 0.2);
  --el-tag-text-color: #{$success};
}

.el-tag--primary {
  --el-tag-bg-color: #{$color-violet-dim};
  --el-tag-border-color: rgba(124, 58, 237, 0.2);
  --el-tag-text-color: #{$color-violet};
}

.el-tag--danger {
  --el-tag-bg-color: rgba(239, 68, 68, 0.1);
  --el-tag-border-color: rgba(239, 68, 68, 0.2);
  --el-tag-text-color: #{$danger};
}

.el-tag--warning {
  --el-tag-bg-color: rgba(245, 158, 11, 0.1);
  --el-tag-border-color: rgba(245, 158, 11, 0.2);
  --el-tag-text-color: #{$warning};
}

// Primary button gradient
.el-button--primary {
  background: linear-gradient(135deg, $color-violet, $color-cyan) !important;
  border: none !important;
  color: #fff !important;
  &:hover, &:focus {
    opacity: 0.9;
    box-shadow: $glow-violet;
  }
  &.is-disabled {
    opacity: 0.4;
    box-shadow: none;
  }
}

.el-rate__icon {
  color: $text-muted;
  &.is-active {
    color: $color-violet;
  }
}

// Input focus glow
.el-input.is-focus .el-input__wrapper,
.el-input__wrapper:focus-within,
.el-textarea.is-focus .el-textarea__inner,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 1px $color-violet !important;
}

.el-input__wrapper {
  background-color: $bg-surface;
  box-shadow: 0 0 0 1px $border-subtle;
}

.el-textarea__inner {
  background-color: $bg-surface;
  color: $text-primary;
  border-color: $border-subtle;
}

// ============================================
// Animations
// ============================================
@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@keyframes pulse-ring {
  0% {
    box-shadow: 0 0 0 0 rgba(124, 58, 237, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(124, 58, 237, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(124, 58, 237, 0);
  }
}

@keyframes gradient-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

// ============================================
// Utility Classes
// ============================================
.gradient-text {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.gradient-border {
  border: 1px solid transparent;
  background-clip: padding-box;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: inherit;
    background: linear-gradient(135deg, rgba(124,58,237,0.3), rgba(6,182,212,0.3));
    z-index: -1;
  }
}

// Scrollbar
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(124, 58, 237, 0.2);
  border-radius: 3px;
  &:hover {
    background: rgba(124, 58, 237, 0.35);
  }
}
```

- [ ] **Step 3: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds with no errors (warnings about SCSS deprecations are OK).

- [ ] **Step 4: Commit**

```bash
git add cat-web/src/assets/styles/_variables.scss cat-web/src/assets/styles/main.scss
git commit -m "feat(ui): dark tech design system + Element Plus theme overrides

- Replace warm-orange SCSS variables with dark palette (#0B0D14 → #252836)
- Add purple-blue gradient tokens (#7C3AED → #06B6D4)
- Override all Element Plus CSS custom properties for dark theme
- Add gradient button, glow input focus, dark tags
- Add breathe/pulse-ring/gradient-shift animations
- Add custom dark scrollbar styling

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: SVG Icon Components

**Files:**
- Create: `cat-web/src/components/icons/CatIcons.vue`

Note: The directory `cat-web/src/components/icons/` must be created before the file. If `mkdir` is not available, create a placeholder `index.ts` first using the `create` tool with path `cat-web/src/components/icons/CatIcons.vue` directly (the tool creates parent directories if the parent path is provided in full).

- [ ] **Step 1: Create CatIcons.vue with all SVG icon components**

Create `cat-web/src/components/icons/CatIcons.vue`:

```vue
<script lang="ts">
// Functional SVG icon components for the dark tech cat theme.
// All icons use stroke style, 24x24 viewBox, stroke-width 1.5.
// Usage: <CatLogo :size="32" color="#7C3AED" />
import { defineComponent, h, type PropType } from 'vue'

const iconProps = {
  size: { type: Number, default: 24 },
  color: { type: String, default: 'currentColor' }
}

function svgWrapper(size: number, children: any[]) {
  return h('svg', {
    xmlns: 'http://www.w3.org/2000/svg',
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': '1.5',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round'
  }, children)
}

export const CatLogo = defineComponent({
  name: 'CatLogo',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M12 22c-4.97 0-9-3.58-9-8 0-2.1.8-4.02 2.14-5.5L3 2l4.5 2.5C8.96 3.9 10.43 3.5 12 3.5s3.04.4 4.5 1L21 2l-2.14 6.5A8.48 8.48 0 0 1 21 14c0 4.42-4.03 8-9 8z' }),
      h('circle', { cx: '9', cy: '13', r: '1', fill: 'currentColor', stroke: 'none' }),
      h('circle', { cx: '15', cy: '13', r: '1', fill: 'currentColor', stroke: 'none' }),
      h('path', { d: 'M10 16s.5 1 2 1 2-1 2-1' })
    ])])
  }
})

export const RobotAgent = defineComponent({
  name: 'RobotAgent',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '4', y: '6', width: '16', height: '12', rx: '3' }),
      h('circle', { cx: '9', cy: '12', r: '1.5', fill: 'currentColor', stroke: 'none' }),
      h('circle', { cx: '15', cy: '12', r: '1.5', fill: 'currentColor', stroke: 'none' }),
      h('path', { d: 'M10 15h4' }),
      h('path', { d: 'M12 2v4' }),
      h('path', { d: 'M2 10h2' }),
      h('path', { d: 'M20 10h2' })
    ])])
  }
})

export const GroupChatIcon = defineComponent({
  name: 'GroupChatIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' }),
      h('circle', { cx: '9', cy: '7', r: '4' }),
      h('path', { d: 'M22 21v-2a4 4 0 0 0-3-3.87' }),
      h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
    ])])
  }
})

export const DashboardIcon = defineComponent({
  name: 'DashboardIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '3', y: '3', width: '7', height: '9', rx: '1.5' }),
      h('rect', { x: '14', y: '3', width: '7', height: '5', rx: '1.5' }),
      h('rect', { x: '14', y: '12', width: '7', height: '9', rx: '1.5' }),
      h('rect', { x: '3', y: '16', width: '7', height: '5', rx: '1.5' })
    ])])
  }
})

export const TerminalIcon = defineComponent({
  name: 'TerminalIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '2', y: '4', width: '20', height: '16', rx: '2' }),
      h('path', { d: 'M6 9l4 3-4 3' }),
      h('path', { d: 'M12 15h6' })
    ])])
  }
})

export const LightningIcon = defineComponent({
  name: 'LightningIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M13 2L3 14h9l-1 8 10-12h-9l1-8z', fill: 'currentColor', 'stroke-width': '0' })
    ])])
  }
})

export const ChartBarIcon = defineComponent({
  name: 'ChartBarIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('rect', { x: '3', y: '12', width: '4', height: '9', rx: '1' }),
      h('rect', { x: '10', y: '7', width: '4', height: '14', rx: '1' }),
      h('rect', { x: '17', y: '3', width: '4', height: '18', rx: '1' })
    ])])
  }
})

export const RefreshIcon = defineComponent({
  name: 'RefreshIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M21 2v6h-6' }),
      h('path', { d: 'M3 12a9 9 0 0 1 15-6.7L21 8' }),
      h('path', { d: 'M3 22v-6h6' }),
      h('path', { d: 'M21 12a9 9 0 0 1-15 6.7L3 16' })
    ])])
  }
})

export const PlusCircleIcon = defineComponent({
  name: 'PlusCircleIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('path', { d: 'M12 8v8' }),
      h('path', { d: 'M8 12h8' })
    ])])
  }
})

export const MessageBubbleIcon = defineComponent({
  name: 'MessageBubbleIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' })
    ])])
  }
})

export const FoldIcon = defineComponent({
  name: 'FoldIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M3 6h18' }),
      h('path', { d: 'M3 12h12' }),
      h('path', { d: 'M3 18h18' }),
      h('path', { d: 'M18 9l3 3-3 3' })
    ])])
  }
})

export const ExpandIcon = defineComponent({
  name: 'ExpandIcon',
  props: iconProps,
  setup(props) {
    return () => h('span', {
      style: { display: 'inline-flex', color: props.color, width: `${props.size}px`, height: `${props.size}px` }
    }, [svgWrapper(props.size, [
      h('path', { d: 'M3 6h18' }),
      h('path', { d: 'M9 12h12' }),
      h('path', { d: 'M3 18h18' }),
      h('path', { d: 'M6 9l-3 3 3 3' })
    ])])
  }
})
</script>
```

- [ ] **Step 2: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds. The icons file exports are importable.

- [ ] **Step 3: Commit**

```bash
git add cat-web/src/components/icons/CatIcons.vue
git commit -m "feat(ui): add SVG icon components for dark tech theme

- CatLogo, RobotAgent, GroupChatIcon, DashboardIcon, TerminalIcon
- LightningIcon, ChartBarIcon, RefreshIcon, PlusCircleIcon
- MessageBubbleIcon, FoldIcon, ExpandIcon
- All stroke-based, 24x24 viewBox, color/size props

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: AppLayout Dark Redesign

**Files:**
- Modify: `cat-web/src/components/layout/AppLayout.vue` (full file)

- [ ] **Step 1: Replace AppLayout.vue with dark tech version**

Replace the entire content of `cat-web/src/components/layout/AppLayout.vue` with:

```vue
<template>
  <div class="app-layout">
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <CatLogo :size="28" />
        <span v-show="!isCollapsed" class="logo-text">猫猫协同</span>
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
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <span class="avatar-ring">
                <el-avatar :size="30" class="user-avatar">
                  {{ displayName?.charAt(0)?.toUpperCase() || 'U' }}
                </el-avatar>
              </span>
              <span class="username">{{ displayName || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  CatLogo,
  DashboardIcon,
  TerminalIcon,
  MessageBubbleIcon,
  FoldIcon,
  ExpandIcon
} from '@/components/icons/CatIcons.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapsed = ref(false)
const displayName = computed(() => authStore.username || '用户')

const menuItems = [
  { path: '/dashboard', label: '仪表盘', icon: DashboardIcon },
  { path: '/cli-agents', label: 'CLI Agent', icon: TerminalIcon },
  { path: '/group-chat', label: '群聊', icon: MessageBubbleIcon }
]

function isMenuActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
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

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
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

.username {
  color: $text-primary;
  font-size: 14px;
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
```

- [ ] **Step 2: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds. No import errors for icons.

- [ ] **Step 3: Commit**

```bash
git add cat-web/src/components/layout/AppLayout.vue
git commit -m "feat(ui): dark tech AppLayout with gradient sidebar and glassmorphism header

- Dark sidebar with gradient logo text and active indicator bar
- Custom nav items replacing el-menu, with gradient left border on active
- SVG cat icons replacing Element Plus icons
- Glassmorphism header with backdrop-filter blur
- Gradient ring around user avatar

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: Dashboard Dark Redesign

**Files:**
- Modify: `cat-web/src/views/dashboard/DashboardView.vue` (full file)

- [ ] **Step 1: Replace DashboardView.vue with dark tech version**

Replace the entire content of `cat-web/src/views/dashboard/DashboardView.vue` with:

```vue
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
} from '@/components/icons/CatIcons.vue'

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
```

- [ ] **Step 2: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add cat-web/src/views/dashboard/DashboardView.vue
git commit -m "feat(ui): dark tech dashboard with gradient stats and SVG icons

- Dark stat cards with gradient icon backgrounds and glow hover
- Gradient text for values and page title
- SVG icons replacing emoji (RobotAgent, Lightning, ChartBar, Refresh)
- Quick action cards with gradient accent and hover effects

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: CLI Agent List Dark Redesign

**Files:**
- Modify: `cat-web/src/views/cliAgent/CliAgentListView.vue` (styles only — `<style>` section)

This task only replaces the `<style>` block. The `<template>` and `<script>` sections are preserved unchanged.

- [ ] **Step 1: Replace CliAgentListView.vue style section**

Replace the entire `<style scoped>` block (from `<style scoped>` to `</style>`, lines 601-711) with:

```html
<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.cli-agent-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
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
  background: $bg-surface;
  border-radius: $radius-md;
  padding: 20px;
  border: 1px solid $border-subtle;
  transition: all 0.25s ease;

  &:hover {
    border-color: $border-active;
    box-shadow: $glow-violet;
    transform: translateY(-1px);
  }
}

.agent-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.agent-icon {
  font-size: 28px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.agent-info {
  flex: 1;
  min-width: 0;
}

.agent-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: $text-primary;
}

.agent-desc {
  font-size: 13px;
  color: $text-secondary;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  padding: 3px 10px;
  border-radius: $radius-sm;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-tag.running {
  background: rgba(6, 182, 212, 0.12);
  color: $status-running;
  animation: breathe 2s ease-in-out infinite;
}

.status-tag.executing {
  background: rgba(124, 58, 237, 0.12);
  color: $status-executing;
  animation: pulse-ring 1.5s ease-out infinite;
}

.status-tag.stopped {
  background: rgba(75, 85, 99, 0.15);
  color: $status-stopped;
}

.status-tag.error {
  background: rgba(239, 68, 68, 0.12);
  color: $status-error;
}

.status-tag.starting {
  background: rgba(245, 158, 11, 0.12);
  color: $warning;
}

.agent-meta {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}

.agent-stats {
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid $border-subtle;
  border-bottom: 1px solid $border-subtle;
  margin-bottom: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat .value {
  font-size: 14px;
  font-weight: 600;
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat .label {
  font-size: 12px;
  color: $text-muted;
}

.agent-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.env-vars-editor {
  width: 100%;
}

.env-var-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.form-hint {
  font-size: 12px;
  color: $text-muted;
  margin-top: 4px;
}
</style>
```

- [ ] **Step 2: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add cat-web/src/views/cliAgent/CliAgentListView.vue
git commit -m "feat(ui): dark tech CLI Agent list with status animations

- Dark agent cards with glow hover and gradient stat values
- Status tags: cyan breathe (running), violet pulse (executing)
- Gradient page title, dark filter bar
- Agent icon placeholder with gradient background

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 6: Group Chat Dark Redesign

**Files:**
- Modify: `cat-web/src/views/groupChat/GroupChatView.vue` (styles only — `<style>` section)

This task only replaces the `<style>` block. The `<template>` and `<script>` sections are preserved unchanged.

- [ ] **Step 1: Replace GroupChatView.vue style section**

Replace the entire `<style scoped>` block (from `<style scoped>` to `</style>`, lines 783-1213) with:

```html
<style lang="scss" scoped>
@use '@/assets/styles/variables' as *;

.group-chat {
  display: flex;
  height: calc(100vh - 64px - 48px);
  background: $bg-surface;
  border-radius: $radius-lg;
  overflow: hidden;
  border: 1px solid $border-subtle;
}

// ===== Left sidebar =====
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

// ===== Right chat area =====
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: $bg-base;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid $border-subtle;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: $bg-surface;
  flex-wrap: wrap;
  gap: 8px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-avatar-large {
  font-size: 28px;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, $color-violet-dim, $color-cyan-dim);
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-text h2 {
  margin: 0;
  font-size: 16px;
  color: $text-primary;
}

.header-text p {
  margin: 2px 0 0;
  font-size: 12px;
  color: $text-muted;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.agent-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.agent-tag {
  font-size: 11px;
}

// ===== Messages =====
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: $bg-base;
}

.empty-chat {
  text-align: center;
  padding: 60px 20px;
  color: $text-muted;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    color: $text-secondary;
  }

  .hint {
    font-size: 12px;
    color: $text-muted;
  }
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  background: $bg-surface;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  border: 1px solid $border-subtle;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  border-color: transparent;
}

.message.system .message-avatar {
  background: $bg-hover;
  border-color: $border-subtle;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.sender {
  font-weight: 600;
  font-size: 13px;
}

.sender-user {
  background: linear-gradient(135deg, $color-violet, $color-cyan);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sender-agent { color: $status-running; }
.sender-system { color: $text-muted; }

.broadcast-tag {
  font-size: 11px;
  color: $warning;
}

.mention-tag {
  font-size: 11px;
  color: $color-violet;
}

.time {
  font-size: 11px;
  color: $text-muted;
}

.message-text {
  background: $bg-surface;
  padding: 10px 14px;
  border-radius: $radius-md;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid $border-subtle;
  color: $text-primary;
  word-break: break-word;

  :deep(code) {
    background: $bg-hover;
    padding: 1px 5px;
    border-radius: 4px;
    font-family: 'JetBrains Mono', 'Fira Code', monospace;
    font-size: 13px;
  }
}

.message.user .message-text {
  background: rgba(124, 58, 237, 0.08);
  border-color: rgba(124, 58, 237, 0.15);
}

.message.system .message-text {
  background: $bg-hover;
  border-color: $border-subtle;
  font-size: 13px;
  color: $text-secondary;
}

.message.agent .message-text {
  background: $bg-surface;
  border-color: $border-subtle;
  border-left: 3px solid $color-violet;
}

.message.streaming .message-text {
  border-color: rgba(124, 58, 237, 0.3);
  animation: breathe 2s infinite;
}

.streaming-indicator {
  font-size: 12px;
  color: $color-violet;
  display: flex;
  align-items: center;
  gap: 4px;
}

.spinner-icon {
  font-weight: bold;
  color: $color-violet;
}

.empty-content {
  color: $text-muted;
}

// ===== @Mention Popup =====
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

// ===== Mention Tags =====
.mention-tags {
  padding: 4px 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
  border-bottom: 1px solid $border-subtle;
}

// ===== Chat Input =====
.chat-input {
  padding: 12px 20px;
  border-top: 1px solid $border-subtle;
  background: $bg-surface;
  position: relative;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.mention-btn {
  font-weight: bold;
  font-size: 16px;
  min-width: 36px;
  flex-shrink: 0;
  margin-top: 4px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.input-hints {
  font-size: 12px;
  color: $text-muted;
}

// ===== No Group Selected =====
.no-group-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $text-muted;
  background: $bg-base;

  .empty-icon {
    font-size: 64px;
    margin-bottom: 20px;
    opacity: 0.4;
  }

  h2 {
    margin: 0 0 8px;
    color: $text-primary;
  }
}

// ===== Agent Selector (in dialog) =====
.agent-selector {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid $border-subtle;
  border-radius: $radius-md;
  padding: 8px;
  background: $bg-surface;
}

.agent-checkbox-item {
  padding: 6px 0;
}

.agent-checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
```

- [ ] **Step 2: Verify build passes**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add cat-web/src/views/groupChat/GroupChatView.vue
git commit -m "feat(ui): dark tech group chat with gradient messages and dark mention

- Dark sidebar with gradient active indicator
- User messages with violet tint, agent messages with purple left bar
- Streaming indicator with violet pulse
- Dark mention popup with gradient highlight
- Code blocks with monospace font in dark bg

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: Build Verification & Final Commit

**Files:**
- None (verification only)

- [ ] **Step 1: Full build verification**

Run: `cd cat-web && npx vite build`
Expected: Build succeeds with no errors.

- [ ] **Step 2: Verify all SCSS imports resolve**

Run: `cd cat-web && npx vite build 2>&1 | findstr /i "error"`
Expected: No output (no errors).

- [ ] **Step 3: Verify no broken imports**

Run: `cd cat-web && npx vite build 2>&1 | findstr /i "Cannot find\|not found\|Module not"`
Expected: No output (no import errors).

- [ ] **Step 4: Create summary commit tag (optional)**

```bash
git log --oneline -6
```

Verify all 6 commits are present in the expected order.
