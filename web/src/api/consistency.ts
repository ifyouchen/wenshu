/**
 * 一致性审查 API（P8-12）。
 * 提交审查任务、查询报告、更新条目状态。
 */
import client from './client'
import type {ApiResponse} from './types'

/** 一致性审查报告条目（对应后端 ConsistencyItemInfo）。 */
export interface ConsistencyReportItem {
  id: string
  type: string | null
  /** 涉及角色名称。 */
  character: string | null
  /** 涉及章节提示（章节标题或描述，非 UUID）。 */
  chapterHint: string | null
  description: string
  suggestion: string | null
  status: 'open' | 'handled' | 'ignored'
  createdAt: string
  updatedAt: string
}

/** 一致性审查报告（对应后端 ConsistencyReportInfo）。 */
export interface ConsistencyReport {
  reportId: string
  projectId: string | null
  totalItems: number
  openItems: number
  createdAt: string
  items: ConsistencyReportItem[]
}

/** 提交一致性审查任务（异步，返回 taskId 和 reportId）。 */
export function submitConsistencyCheck(projectId: string) {
  return client.post<ApiResponse<{ taskId: string; reportId: string }>>(
    `/consistency/check?projectId=${projectId}`,
  )
}

/** 查询一致性审查报告。 */
export function getConsistencyReport(reportId: string) {
  return client.get<ApiResponse<ConsistencyReport>>(`/consistency/reports/${reportId}`)
}

/** 更新审查条目状态（open/handled/ignored）。 */
export function updateItemStatus(itemId: string, status: 'open' | 'handled' | 'ignored') {
  return client.patch<ApiResponse<void>>(`/consistency/items/${itemId}`, { status })
}

