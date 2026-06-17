/** 剧本草稿、场景、集数相关 API（P8-13）。 */
import client from './client'
import type { ApiResponse, ScriptDraftInfo, ScriptEpisodeInfo, ScriptSceneInfo } from './types'

export interface ScenePageResult {
  total: number
  page: number
  size: number
  scenes: ScriptSceneInfo[]
}

/** 提交剧本改编任务。 */
export function convertScript(data: {
  projectId: string
  title?: string
  psychologyStrategy?: string
}) {
  return client.post<ApiResponse<{ taskId: string; draftId: string }>>('/script/convert', data)
}

/** 查询作品下的草稿列表。 */
export function listDrafts(projectId: string) {
  return client.get<ApiResponse<ScriptDraftInfo[]>>(`/script/projects/${projectId}/drafts`)
}

/** 获取草稿详情。 */
export function getDraft(draftId: string) {
  return client.get<ApiResponse<ScriptDraftInfo>>(`/script/drafts/${draftId}`)
}

/** 分页查询草稿场景。 */
export function listScenes(draftId: string, page = 0, size = 20) {
  return client.get<ApiResponse<ScenePageResult>>(
    `/script/drafts/${draftId}/scenes?page=${page}&size=${size}`,
  )
}

/** 更新场景内容（含乐观锁）。 */
export function updateScene(
  sceneId: string,
  data: { content?: string; location?: string; timeDesc?: string; version: number },
) {
  return client.put<ApiResponse<ScriptSceneInfo>>(`/script/scenes/${sceneId}`, data)
}

/** 提交导出任务。 */
export function exportDraft(draftId: string, format: 'docx' | 'fdx' | 'storyboard' = 'docx') {
  return client.post<ApiResponse<{ taskId: string; draftId: string }>>(
    `/script/drafts/${draftId}/export?format=${format}`,
  )
}

/** 创建剧本分集。 */
export function createEpisode(draftId: string, data: { episodeNo: number; title: string }) {
  return client.post<ApiResponse<ScriptEpisodeInfo>>(`/script/drafts/${draftId}/episodes`, data)
}

/** 查询草稿下的分集列表。 */
export function listEpisodes(draftId: string) {
  return client.get<ApiResponse<ScriptEpisodeInfo[]>>(`/script/drafts/${draftId}/episodes`)
}

/** 删除剧本分集。 */
export function deleteEpisode(draftId: string, episodeId: string) {
  return client.delete<ApiResponse<void>>(`/script/drafts/${draftId}/episodes/${episodeId}`)
}
