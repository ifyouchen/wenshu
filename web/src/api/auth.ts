/** 认证相关 API（登录/注册/刷新/登出）。 */
import client from './client'
import type { ApiResponse, UserInfo } from './types'

export interface RegisterResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  refreshExpiresIn: number
  user: UserInfo
}

/** 发送注册验证码。 */
export function sendRegisterCode(email: string) {
  return client.post<ApiResponse<{ sent: boolean; expiresAt: string }>>('/auth/register/code', { email })
}

/** 邮箱验证码注册。 */
export function register(email: string, password: string, nickname: string, verificationCode: string) {
  return client.post<ApiResponse<RegisterResult>>('/auth/register', {
    email,
    password,
    nickname,
    verificationCode,
  })
}

/** 邮箱密码登录。 */
export function login(email: string, password: string) {
  return client.post<ApiResponse<RegisterResult>>('/auth/login', { email, password })
}

/** Refresh Token 轮换。 */
export function refreshToken(refreshToken: string) {
  return client.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
    '/auth/refresh',
    { refreshToken },
  )
}

/** 登出当前设备。 */
export function logout() {
  return client.post<ApiResponse<void>>('/auth/logout')
}

/** 发起密码重置邮件。 */
export function forgotPassword(email: string) {
  return client.post<ApiResponse<{ message: string }>>('/auth/password/forgot', { email })
}

/** 重置密码。 */
export function resetPassword(token: string, newPassword: string) {
  return client.post<ApiResponse<void>>('/auth/password/reset', { token, newPassword })
}
