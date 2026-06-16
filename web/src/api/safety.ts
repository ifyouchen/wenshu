/**
 * 内容安全与申诉 API（P8-17 / P9-05）。
 */
import client from './client'
import type { ApiResponse } from './types'

/** 内容安全策略信息。 */
export interface ContentPolicy {
  safetyLevels: Array<{ level: string; description: string }>
  copyright: { ownership: string; disclaimer: string; exportAnnotation: string }
  privacy: { dataUsage: string; aiTraining: string; retention: string }
  appealProcess: string
}

/** 申诉记录信息。 */
export interface AppealInfo {
  id: string
  content: string
  reason: string
  status: 'pending' | 'approved' | 'rejected'
  reviewerNote: string | null
  createdAt: string
}

/** 查询内容安全策略（无需鉴权）。 */
export function getContentPolicy() {
  return client.get<ApiResponse<ContentPolicy>>('/content/policy')
}

/** 提交内容安全误报申诉。 */
export function submitContentAppeal(content: string, reason: string) {
  return client.post<ApiResponse<AppealInfo>>('/content/appeals', { content, reason })
}

/** 查询当前用户申诉历史。 */
export function listContentAppeals() {
  return client.get<ApiResponse<AppealInfo[]>>('/content/appeals')
}
