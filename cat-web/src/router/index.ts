import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue')
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
        path: 'group-chat',
        name: 'GroupChat',
        component: () => import('@/views/groupChat/GroupChatView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
