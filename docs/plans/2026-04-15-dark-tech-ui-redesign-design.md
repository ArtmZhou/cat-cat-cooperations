# 猫猫Agent协同系统 — 深色科技风UI重设计

## 概述

将现有的暖色调浅色UI改版为深色科技风，使用紫蓝渐变（#7C3AED → #06B6D4）作为主色调，保留精致的猫咪元素融入科技风格。采用混合定制方案：自定义壳组件实现暗色科技风，保留Element Plus的表单/对话框等实用组件。

## 改版范围

- ✅ AppLayout（侧边栏 + 头部）
- ✅ 仪表盘 (DashboardView)
- ✅ CLI Agent列表 (CliAgentListView)
- ✅ 群聊页面 (GroupChatView)
- ✅ 全局样式系统 (_variables.scss, main.scss)
- ✅ Element Plus 暗色主题覆盖
- ❌ 登录页 (保持不变)
- ❌ Agent详情页、用户管理等次要页面 (后续迭代)

## 设计系统

### 色彩体系

#### 背景层级
```
--bg-deep:     #0B0D14    // 最深层：侧边栏
--bg-base:     #0F1117    // 主背景
--bg-surface:  #161823    // 卡片/面板
--bg-elevated: #1C1F2E    // 弹窗/悬浮
--bg-hover:    #252836    // 悬停态
```

#### 主色渐变
```
--gradient-primary: linear-gradient(135deg, #7C3AED, #06B6D4)
--color-violet:     #7C3AED
--color-cyan:       #06B6D4
--color-violet-dim: rgba(124, 58, 237, 0.15)
--color-cyan-dim:   rgba(6, 182, 212, 0.15)
```

#### 文字层级
```
--text-primary:   #F0F0F0    // 主要文字
--text-secondary: #9CA3AF    // 次要文字
--text-muted:     #4B5563    // 禁用/提示
--text-accent:    渐变文字     // 强调（使用 background-clip: text）
```

#### 语义色
```
--status-running:   #06B6D4 (青色发光)
--status-executing: #7C3AED (紫色脉冲动画)
--status-stopped:   #4B5563 (暗灰)
--status-error:     #EF4444 (红色)
--success:          #10B981
--warning:          #F59E0B
--danger:           #EF4444
```

#### 边框与阴影
```
--border-subtle:  rgba(124, 58, 237, 0.12)
--border-active:  rgba(124, 58, 237, 0.4)
--glow-violet:    0 0 20px rgba(124, 58, 237, 0.15)
--glow-cyan:      0 0 20px rgba(6, 182, 212, 0.15)
--glow-strong:    0 0 30px rgba(124, 58, 237, 0.25), 0 0 60px rgba(6, 182, 212, 0.1)
```

### 间距
保持现有间距系统：4/8/16/24/32px

### 圆角
- 小: 6px (标签、小按钮)
- 中: 10px (卡片、输入框)
- 大: 14px (面板、弹窗)
- 超大: 20px (登录卡片等大容器)

### 字体
保持 'Inter', 'PingFang SC', sans-serif，增加等宽字体用于代码/终端区域：'JetBrains Mono', 'Fira Code', monospace

## 组件设计

### 1. AppLayout 侧边栏

**背景:** `--bg-deep` (#0B0D14)，右侧边框使用 `--border-subtle`

**Logo区域:**
- SVG猫耳轮廓图标（描边风格，渐变色）
- "猫猫协同" 文字使用渐变色 (`background-clip: text`)
- 底部分割线使用渐变色淡出

**菜单项:**
- 默认: 图标 + 文字，`--text-secondary` 颜色
- 悬停: 背景 `--bg-hover`，文字变亮
- 激活: 左侧 3px 渐变竖条，背景 `rgba(124,58,237,0.08)`，文字 `--text-primary`，图标渐变色
- 折叠动画: `width` 过渡 0.3s ease

**图标:** 用SVG内联图标替代Element Plus图标，描边风格

### 2. AppLayout 头部

**背景:** 透明 / `rgba(15,17,23,0.8)` + `backdrop-filter: blur(12px)`
**底部边框:** `1px solid var(--border-subtle)`
**折叠按钮:** 文字按钮，悬停渐变色
**用户区域:** 头像带渐变边框圆环（2px渐变border），用户名白色

### 3. 仪表盘 (DashboardView)

**页面标题:** 大字白色，或渐变文字

**统计卡片:**
- 背景: `--bg-surface`
- 边框: `1px solid var(--border-subtle)`
- 悬停: 边框颜色增强至 `--border-active`，轻微上移 `translateY(-2px)`，添加 `--glow-violet`
- 图标区域: 渐变背景圆角方块（替代原有浅色背景）
- 数值: 大号渐变色文字
- 使用SVG图标替代emoji (🤖→机器人SVG, ⚡→闪电SVG, 📊→图表SVG, 🔄→刷新SVG)

**快速操作卡片:**
- 同上卡片风格
- 悬停时边框渐变动画（从subtle到gradient border）
- 图标: 渐变色SVG

### 4. CLI Agent列表 (CliAgentListView)

**页面头部:**
- 标题白色
- 创建按钮: 渐变背景，悬停发光增强

**过滤栏:**
- 输入框/下拉框: 暗色背景 `--bg-surface`，边框 `--border-subtle`
- 聚焦时边框渐变色

**Agent卡片网格:**
- 背景: `--bg-surface`
- 边框: `1px solid var(--border-subtle)`
- 头部区域: SVG机器人图标（替代emoji），Agent名称白色，描述灰色
- 状态标签: 使用语义色 + 发光效果
  - RUNNING: 青色背景透明 + 青色文字 + 微弱呼吸动画
  - EXECUTING: 紫色背景透明 + 紫色文字 + 脉冲动画
  - STOPPED: 暗灰背景 + 灰色文字
  - ERROR: 红色背景透明 + 红色文字
- 元数据标签: 暗色背景 + 渐变边框
- 统计数据行: 暗色分割，数值渐变色
- 操作按钮行: 暗色按钮，主要操作渐变色

### 5. 群聊页面 (GroupChatView)

**左侧群组列表:**
- 背景: `--bg-deep`
- 头部: "群聊"标题 + 渐变新建按钮
- 群组项: 
  - 默认: `--bg-deep`
  - 悬停: `--bg-hover`
  - 激活: `--bg-surface` + 左侧渐变竖条
  - 头像: SVG群组图标（替代👥emoji）

**聊天头部:**
- 背景: `--bg-surface` + 底部边框
- Agent标签: 暗色底 + 对应状态颜色

**消息区域:**
- 背景: `--bg-base`
- 用户消息: 渐变背景气泡（紫→青，低透明度），右对齐
- Agent消息: `--bg-surface` 背景 + 左侧3px紫色竖条，左对齐
- 消息头像: 用户=渐变圆环头像，Agent=SVG机器人图标
- 流式输出指示器: 紫色脉冲点动画（替代spinner emoji）
- 时间戳: `--text-muted`

**输入区域:**
- 背景: `--bg-surface`
- 输入框: `--bg-elevated` 背景，聚焦渐变边框
- @提及弹窗: `--bg-elevated` + 渐变边框，选中项渐变背景
- 发送按钮: 渐变背景，禁用态暗灰

## 猫咪SVG图标集

所有emoji替换为内联SVG组件，统一描边风格（stroke-width: 1.5px），默认使用currentColor继承文字颜色，激活态使用渐变色。

需要的图标:
1. **CatLogo** - 猫耳轮廓 (侧边栏Logo)
2. **CatPaw** - 猫爪 (装饰元素)
3. **RobotAgent** - 机器人/Agent (替代🤖)
4. **GroupChat** - 群组聊天 (替代👥)
5. **Dashboard** - 仪表盘图标 (替代DataAnalysis)
6. **Terminal** - 终端/CLI (替代Monitor)
7. **Lightning** - 闪电/执行中 (替代⚡)
8. **ChartBar** - 图表 (替代📊)
9. **Refresh** - 刷新/并发 (替代🔄)
10. **PlusCircle** - 创建/添加 (替代➕)
11. **MessageBubble** - 消息 (替代💬)

## Element Plus 暗色覆盖

通过CSS Custom Properties覆盖Element Plus默认主题：

```scss
:root {
  // Element Plus 暗色覆盖
  --el-bg-color: #161823;
  --el-bg-color-overlay: #1C1F2E;
  --el-text-color-primary: #F0F0F0;
  --el-text-color-regular: #9CA3AF;
  --el-text-color-secondary: #4B5563;
  --el-border-color: rgba(124, 58, 237, 0.12);
  --el-border-color-light: rgba(124, 58, 237, 0.08);
  --el-fill-color-blank: #161823;
  --el-color-primary: #7C3AED;
  --el-mask-color: rgba(11, 13, 20, 0.8);
  --el-dialog-bg-color: #1C1F2E;
  --el-input-bg-color: #161823;
  // ... 其余覆盖
}
```

## 动画效果

1. **呼吸灯:** RUNNING状态标签微弱透明度脉冲 (2s infinite)
2. **脉冲:** EXECUTING状态紫色光圈扩散动画
3. **悬停上移:** 卡片悬停 translateY(-2px) + 阴影增强 (0.2s ease)
4. **渐变边框:** 悬停时边框从subtle到active渐变过渡
5. **菜单激活:** 左侧竖条滑入动画 (0.2s ease)
6. **输入聚焦:** 边框渐变色过渡

## 文件变更清单

1. `src/assets/styles/_variables.scss` — 替换为暗色设计系统变量
2. `src/assets/styles/main.scss` — 更新全局样式 + Element Plus覆盖
3. `src/components/layout/AppLayout.vue` — 重写侧边栏/头部样式与SVG图标
4. `src/views/dashboard/DashboardView.vue` — 暗色统计卡片 + SVG图标
5. `src/views/cliAgent/CliAgentListView.vue` — 暗色Agent卡片 + 状态动画
6. `src/views/groupChat/GroupChatView.vue` — 暗色聊天界面 + 消息气泡
7. `src/components/icons/` (新目录) — SVG猫咪图标组件集

## 实现约束

- 不引入新的CSS框架(如Tailwind)，使用SCSS + CSS Custom Properties
- 保留Element Plus表单/对话框/表格组件，仅覆盖主题色
- 不改变任何业务逻辑和API调用
- 保持响应式布局
- SVG图标作为Vue SFC组件，支持props传入颜色和尺寸
