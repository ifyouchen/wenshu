/** 内容导入 API：文件解析、章节预览调整、确认导入、粘贴导入。 */
import client from './client'
import type { ApiResponse } from './types'
import type { ChapterInfo } from './project'

export interface ChapterPreviewItem {
  index: number
  title: string
  contentPreview: string
  wordCount: number
}

export interface ImportPreviewInfo {
  parseId: string
  totalChapters: number
  chapters: ChapterPreviewItem[]
  expiresAt: string
}

export interface AdjustChapterItem {
  title: string
  content: string
}

/** 上传 TXT/DOCX 并解析章节预览。 */
export function parseImportFile(projectId: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return client.post<ApiResponse<ImportPreviewInfo>>(
    `/import/parse?projectId=${projectId}`,
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}

/** 调整解析后的章节切分。 */
export function adjustImport(parseId: string, chapters: AdjustChapterItem[]) {
  return client.put<ApiResponse<ImportPreviewInfo>>(`/import/${parseId}/adjust`, { chapters })
}

/** 确认导入到指定卷。 */
export function applyImport(parseId: string, volumeId: string) {
  return client.post<ApiResponse<ChapterInfo[]>>(`/import/${parseId}/apply`, { volumeId })
}

/** 粘贴文本并直接导入到指定卷。 */
export function pasteImport(projectId: string, volumeId: string, text: string) {
  return client.post<ApiResponse<ChapterInfo[]>>('/import/paste', { projectId, volumeId, text })
}
