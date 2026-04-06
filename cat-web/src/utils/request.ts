import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 简化：不再添加认证header
instance.interceptors.request.use(
  (config) => {
    // 简化：不需要token验证
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message, data } = response.data
    if (code === 0) {
      return data as any
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    // 简化：不再自动跳转登录页
    ElMessage.error(error.response?.data?.message || '网络错误')
    return Promise.reject(error)
  }
)

const request = {
  get<T>(url: string, config?: object): Promise<T> {
    return instance.get(url, config)
  },
  post<T>(url: string, data?: unknown, config?: object): Promise<T> {
    return instance.post(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: object): Promise<T> {
    return instance.put(url, data, config)
  },
  delete<T>(url: string, config?: object): Promise<T> {
    return instance.delete(url, config)
  }
}

export default request
export { request }