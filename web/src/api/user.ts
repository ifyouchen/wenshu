/** 用户资料、配额相关 API。 */
import client from './client'
import type { ApiResponse, UserInfo, QuotaInfo, TaskProgress } from './types'

/** 获取当前用户信息。 */
export function getMe() {
  return client.get<ApiResponse<UserInfo>>('/user/me')
}

/** 更新用户资料。 */
export function updateProfile(data: { nickname?: string; avatarUrl?: string; identityType?: string }) {
  return client.put<ApiResponse<UserInfo>>('/user/profile', data)
}

/** 修改密码。 */
export function changePassword(data: { currentPassword: string; newPassword: string }) {
  return client.put<ApiResponse<void>>('/user/password', data)
}

/** 获取当月配额详情。 */
export function getQuota() {
  return client.get<ApiResponse<QuotaInfo>>('/user/quota')
}

/** 设置全局每日目标字数。 */
export function setWritingGoal(dailyCharGoal: number) {
  return client.put<ApiResponse<UserInfo>>('/user/writing-goal', { dailyCharGoal })
}

/** 更新内容改进授权开关。 */
export function updateAiConsent(aiTrainConsent: boolean) {
  return client.put<ApiResponse<UserInfo>>('/user/ai-consent', { aiTrainConsent })
}

/** 设置首次登录身份类型与入口偏好。 */
export function setIdentityType(identityType: string) {
  return client.put<ApiResponse<UserInfo>>('/user/identity-type', { identityType })
}

/** 注销账号（软删除，返回 30 天恢复令牌）。 */
export function deleteAccount() {
  return client.delete<ApiResponse<{ restoreToken: string }>>('/user')
}

/** 查询异步任务进度。 */
export function getTaskProgress(taskId: string) {
  return client.get<ApiResponse<TaskProgress>>(`/tasks/${taskId}/progress`)
}
