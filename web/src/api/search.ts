/** 全书搜索与替换 API（P8-09）。 */
import client from './client'
import type { ApiResponse } from './types'

export interface SearchResult {
  total: number
  chapters: ChapterSearchResult[]
}

export interface ChapterSearchResult {
  chapterId: string
  chapterTitle: string
  matchCount: number
  matches: { before: string; match: string; after: string }[]
}

export interface ReplaceResult {
  totalReplaced: number
  affectedChapters: { chapterId: string; chapterTitle: string; replacedCount: number; snapshotId: string }[]
  characterNameSynced: boolean
}

/** 全书搜索（后端执行，前端不加载全卷）。 */
export function searchProject(projectId: string, keyword: string, caseSensitive = false, wholeWord = false) {
  return client.get<ApiResponse<SearchResult>>(
    `/projects/${projectId}/search?keyword=${encodeURIComponent(keyword)}&caseSensitive=${caseSensitive}&wholeWord=${wholeWord}`,
  )
}

/** 全书替换（替换前自动创建快照）。 */
export function replaceProject(
  projectId: string,
  data: {
    keyword: string
    replacement: string
    caseSensitive?: boolean
    wholeWord?: boolean
    syncCharacterName?: boolean
  },
) {
  return client.post<ApiResponse<ReplaceResult>>(`/projects/${projectId}/search/replace`, data)
}
