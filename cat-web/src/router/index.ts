import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: false },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
      },
      {
        path: 'chat',
        name: 'ChatRoom',
        component: () => import('@/views/chat/ChatRoomView.vue')
      },
      {
        path: 'cli-agents',
        name: 'CliAgentList',
        component: () => import('@/views/cliAgent/CliAgentListView.vue')
      },
      {
        path: 'cli-agents/:id',
        name: 'CliAgentDetail',
        component: () => import('@/views/cliAgent/CliAgentDetailView.vue')
      },
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/views/agent/AgentListView.vue')
      },
      {
        path: 'agents/:id',
        name: 'AgentDetail',
        component: () => import('@/views/agent/AgentDetailView.vue')
      },
      {
        path: 'tasks',
        name: 'TaskList',
        component: () => import('@/views/task/TaskListView.vue')
      },
      {
        path: 'tasks/:id',
        name: 'TaskDetail',
        component: () => import('@/views/task/TaskDetailView.vue')
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/MonitorView.vue')
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/user/UserManagementView.vue')
      },
      {
        path: 'roles',
        name: 'RoleManagement',
        component: () => import('@/views/user/RoleManagementView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 简化：去掉路由守卫，直接允许访问所有页面
// router.beforeEach((to, from, next) => {
//   const authStore = useAuthStore()
//   if (to.meta.requiresAuth && !authStore.isAuthenticated) {
//     next('/login')
//   } else if (to.path === '/login' && authStore.isAuthenticated) {
//     next('/dashboard')
//   } else {
//     next()
//   }
// })

export default router