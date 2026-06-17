/**
 * Axios HTTP 客户端（P8-02）。
 *
 * Token 刷新队列锁机制：
 *  - 多个并发请求同时收到 401 时，只发出一个 refresh 请求。
 *  - 其余请求进入队列，refresh 完成后统一重放。
 *  - refresh 失败时清空 Token 并跳转到登录页。
 */
import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from './types'

/** 正在刷新标志，防止重复 refresh。 */
let isRefreshing = false

/** 等待 refresh 完成的请求队列（每项包含 resolve/reject）。 */
type QueueItem = {
  resolve: (token: string) => void
  reject: (err: unknown) => void
}
const refreshQueue: QueueItem[] = []

/** 刷新成功后，统一消费队列中的等待请求。 */
function flushQueue(newToken: string): void {
  refreshQueue.splice(0).forEach(({ resolve }) => resolve(newToken))
}

/** 刷新失败时，统一拒绝队列中的等待请求。 */
function rejectQueue(err: unknown): void {
  refreshQueue.splice(0).forEach(({ reject }) => reject(err))
}

/** 从本地存储读取访问令牌。 */
export function getAccessToken(): string | null {
  return localStorage.getItem('accessToken')
}

/** 持久化令牌对到本地存储。 */
export function saveTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem('accessToken', accessToken)
  localStorage.setItem('refreshToken', refreshToken)
}

/** 清除所有本地令牌（登出时调用）。 */
export function clearTokens(): void {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

/** Axios 实例，所有业务接口均通过此实例调用。 */
const client = axios.create({
  baseURL: '/api/v1',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

/** 请求拦截器：自动附加 Bearer Token。 */
client.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && config.headers) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

/** 响应拦截器：遇到 401 时静默刷新 Token 并重放原始请求。 */
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error)
    }

    if (localStorage.getItem('wenshu-demo-mode') === '1') {
      return Promise.reject(error)
    }

    original._retry = true

    if (isRefreshing) {
      // 已在刷新中：将当前请求加入队列等待新 Token
      return new Promise<string>((resolve, reject) => {
        refreshQueue.push({ resolve, reject })
      }).then((newToken) => {
        if (original.headers) original.headers['Authorization'] = `Bearer ${newToken}`
        return client(original)
      })
    }

    isRefreshing = true

    try {
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        clearTokens()
        if (window.location.pathname !== '/login') window.location.href = '/login'
        throw new Error('无 Refresh Token')
      }

      // 调用刷新接口（不走拦截器，避免死循环）
      const res = await axios.post<ApiResponse<{
        accessToken: string
        refreshToken: string
      }>>('/api/v1/auth/refresh', { refreshToken })

      const { accessToken, refreshToken: newRefresh } = res.data.data
      saveTokens(accessToken, newRefresh)
      flushQueue(accessToken)

      if (original.headers) original.headers['Authorization'] = `Bearer ${accessToken}`
      return client(original)
    } catch (refreshErr) {
      rejectQueue(refreshErr)
      clearTokens()
      // 跳转到登录页（无法直接访问 router，用 location）
      if (window.location.pathname !== '/login') window.location.href = '/login'
      return Promise.reject(refreshErr)
    } finally {
      isRefreshing = false
    }
  },
)

export default client
