/**
 * 认证 Store（P8-03）。
 * 管理用户信息、Token 持久化、登录/注册/登出操作。
 * isLoggedIn 是全局路由守卫的唯一判断依据。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { getMe } from '@/api/user'
import { saveTokens, clearTokens, getAccessToken } from '@/api/client'
import type { UserInfo } from '@/api/types'

export const useAuthStore = defineStore('auth', () => {
  /** 当前登录用户信息，null 表示未登录。 */
  const user = ref<UserInfo | null>(null)

  /** 是否已登录（本地有 Token 即视为登录态，路由守卫使用此值）。 */
  const isLoggedIn = computed(() => !!getAccessToken() || !!user.value)

  /**
   * 登录。
   * @param email    邮箱
   * @param password 密码
   */
  async function loginAction(email: string, password: string): Promise<void> {
    const res = await authApi.login(email, password)
    const data = res.data.data
    saveTokens(data.accessToken, data.refreshToken)
    user.value = data.user
  }

  /**
   * 注册。
   * @param email    邮箱
   * @param password 密码
   * @param nickname 昵称
   */
  async function registerAction(email: string, password: string, nickname: string): Promise<void> {
    const res = await authApi.register(email, password, nickname)
    const data = res.data.data
    saveTokens(data.accessToken, data.refreshToken)
    user.value = data.user
  }

  /**
   * 登出当前设备，清除本地 Token 和用户信息。
   */
  async function logoutAction(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      clearTokens()
      user.value = null
    }
  }

  /**
   * 从后端拉取最新用户信息（页面刷新后恢复状态时调用）。
   */
  async function fetchUser(): Promise<void> {
    if (!getAccessToken()) return
    try {
      const res = await getMe()
      user.value = res.data.data
    } catch {
      // Token 已失效，清空
      clearTokens()
      user.value = null
    }
  }

  return { user, isLoggedIn, loginAction, registerAction, logoutAction, fetchUser }
})
