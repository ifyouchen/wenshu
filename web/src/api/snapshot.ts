/**
 * 版本快照 API（P8-11）。
 * 支持列表、手动创建、恢复快照。
 */
import client from './client'
import type {ApiResponse} from './types'

/** 快照信息。 */
export interface SnapshotInfo {
  id: string
  chapterId: string
  label: string | null
  /** 快照类型枚举（P1-2）：manual / auto_before_ai / polish_accepted / auto_before_replace / auto_before_restore */
  snapshotType: string
  wordCount: number
  content: string | null
  createdAt: string
}

/** 获取章节快照列表。 */
export function listSnapshots(chapterId: string) {
  return client.get<ApiResponse<SnapshotInfo[]>>(`/chapters/${chapterId}/snapshots`)
}

/** 手动创建快照（snapshotType 默认为 manual）。 */
export function createSnapshot(chapterId: string, label?: string, snapshotType = 'manual') {
  return client.post<ApiResponse<SnapshotInfo>>(`/chapters/${chapterId}/snapshots`, {
    snapshotType,
    label,
  })
}

/** 恢复快照（恢复前自动创建当前状态快照）。 */
export function restoreSnapshot(snapshotId: string) {
  return client.post<ApiResponse<{ chapterId: string }>>(`/snapshots/${snapshotId}/restore`)
}

