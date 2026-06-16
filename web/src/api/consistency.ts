/**
 * 一致性审查 API（P8-12）。
 * 提交审查任务、查询报告、更新条目状态。
 */
import client from './client'
import type {ApiResponse} from './types'

/** 一致性审查报告条目。 */
export interface ConsistencyReportItem {
  id: string
  reportId: string
  type: string | null
  description: string | null
  chapterId: string | null
  chapterTitle: string | null
  severity: string | null
  status: 'open' | 'handled' | 'ignored'
  createdAt: string
}

/** 一致性审查报告。 */
export interface ConsistencyReport {
  id: string
  projectId: string
  status: string
  summary: string | null
  items: ConsistencyReportItem[]
  createdAt: string
}

/** 提交一致性审查任务请求。 */
export interface CheckConsistencyRequest {
  projectId: string
  scope?: string
}

/** 提交一致性审查任务（异步，返回 taskId 和 reportId）。 */
export function submitConsistencyCheck(data: CheckConsistencyRequest) {
  return client.post<ApiResponse<{ taskId: string; reportId: string }>>('/consistency/check', data)
}

/** 查询一致性审查报告。 */
export function getConsistencyReport(reportId: string) {
  return client.get<ApiResponse<ConsistencyReport>>(`/consistency/reports/${reportId}`)
}

/** 更新审查条目状态（open/handled/ignored）。 */
export function updateItemStatus(itemId: string, status: 'open' | 'handled' | 'ignored') {
  return client.patch<ApiResponse<void>>(`/consistency/items/${itemId}`, { status })
}

