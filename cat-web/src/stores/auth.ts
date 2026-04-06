import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 简化：从localStorage读取token
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))

  // 简化：只要有token就认为已认证
  const isAuthenticated = computed(() => !!token.value)

  // 简化：登录时直接设置token
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUsername(name: string) {
    username.value = name
    localStorage.setItem('username', name)
  }

  // 简化：登出时清除token
  function logout() {
    token.value = null
    username.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return {
    token,
    username,
    isAuthenticated,
    setToken,
    setUsername,
    logout
  }
})