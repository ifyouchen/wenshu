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

/** 获取章节详情。 */
export function getChapter(chapterId: string) {
  return client.get<ApiResponse<ChapterInfo>>(`/chapters/${chapterId}`)
}

/** 保存章节内容。 */
export function saveChapter(
  chapterId: string,
  data: { title?: string; content?: string; outline?: string; status?: string },
) {
  return client.put<ApiResponse<ChapterInfo>>(`/chapters/${chapterId}`, data)
}
