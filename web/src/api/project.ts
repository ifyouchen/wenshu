/** 作品、卷、章节 API。 */
import client from './client'
import type { ApiResponse, ProjectInfo } from './types'

export interface ChapterInfo {
  id: string
  volumeId: string
  projectId: string
  title: string | null
  outline: string | null
  content: string
  wordCount: number
  sortOrder: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface OutlineInfo {
  volumes: Array<{
    id: string
    title: string | null
    conflict: string | null
    sortOrder: number
    chapters: Array<{
      id: string
      title: string | null
      outline: string | null
      wordCount: number
      status: string
    }>
  }>
}

export interface VolumeInfo {
  id: string
  projectId: string
  title: string | null
  conflict: string | null
  sortOrder: number
  createdAt?: string
  updatedAt?: string
}

export interface ChapterContextInfo {
  chapter: ChapterInfo
  outline: OutlineInfo | null
  characters: unknown[]
  worldElements: unknown[]
  keyEvents: unknown[]
}

/** 查询作品列表。 */
export function listProjects() {
  return client.get<ApiResponse<ProjectInfo[]>>('/projects')
}

/** 创建作品。 */
export function createProject(data: { title: string; genre?: string; synopsis?: string; worldview?: string }) {
  return client.post<ApiResponse<ProjectInfo>>('/projects', data)
}

/** 获取作品详情。 */
export function getProject(projectId: string) {
  return client.get<ApiResponse<ProjectInfo>>(`/projects/${projectId}`)
}

/** 更新作品。 */
export function updateProject(projectId: string, data: Partial<ProjectInfo>) {
  return client.put<ApiResponse<ProjectInfo>>(`/projects/${projectId}`, data)
}

/** 删除作品。 */
export function deleteProject(projectId: string) {
  return client.delete<ApiResponse<void>>(`/projects/${projectId}?confirm=true`)
}

/** 获取大纲树。 */
export function getOutline(projectId: string) {
  return client.get<ApiResponse<OutlineInfo>>(`/projects/${projectId}/outline`)
}

/** 创建卷。 */
export function createVolume(projectId: string, data: { title: string; conflict?: string }) {
  return client.post<ApiResponse<VolumeInfo>>(`/projects/${projectId}/volumes`, data)
}

/** 更新卷。 */
export function updateVolume(volumeId: string, data: { title?: string; conflict?: string; sortOrder?: number }) {
  return client.put<ApiResponse<VolumeInfo>>(`/volumes/${volumeId}`, data)
}

/** 删除卷。 */
export function deleteVolume(volumeId: string) {
  return client.delete<ApiResponse<void>>(`/volumes/${volumeId}`)
}

/** 创建章节。 */
export function createChapter(volumeId: string, data: { title: string; outline?: string }) {
  return client.post<ApiResponse<ChapterInfo>>(`/volumes/${volumeId}/chapters`, data)
}

/** 删除章节。 */
export function deleteChapter(chapterId: string) {
  return client.delete<ApiResponse<void>>(`/chapters/${chapterId}`)
}

/** 获取章节详情。 */
export function getChapter(chapterId: string) {
  return client.get<ApiResponse<ChapterInfo>>(`/chapters/${chapterId}`)
}

/** 获取章节上下文聚合信息。 */
export function getChapterContext(chapterId: string) {
  return client.get<ApiResponse<ChapterContextInfo>>(`/chapters/${chapterId}/context`)
}

/** 保存章节内容。 */
export function saveChapter(
  chapterId: string,
  data: { title?: string; content?: string; outline?: string; status?: string },
) {
  return client.put<ApiResponse<ChapterInfo>>(`/chapters/${chapterId}`, data)
}

/** 接受辅助生成内容：创建快照 + 记录 ai_accepted_chars（P0-1 修复）*/
export function acceptAiContent(chapterId: string, acceptedChars: number, content: string) {
  return client.post<ApiResponse<{ id: string; snapshotType: string }>>(`/chapters/${chapterId}/accept-ai`, {
    acceptedChars,
    content,
  })
}
