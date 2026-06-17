import client from './client'
import type { ApiResponse } from './types'
import type { ChapterInfo } from './project'

export type RewriteMode = 'polish' | 'expand' | 'shorten' | 'custom'

export interface RewriteSuggestion {
  mode: RewriteMode
  source: string
  rewritten: string
  instruction: string
}

export function rewriteChapter(
  chapterId: string,
  data: { mode: RewriteMode; instruction?: string; selectedText?: string },
) {
  return client.post<ApiResponse<RewriteSuggestion>>(`/chapters/${chapterId}/rewrite`, data)
}

export function applyRewrite(
  chapterId: string,
  data: { content: string; acceptedChars?: number; snapshotLabel?: string },
) {
  return client.post<ApiResponse<ChapterInfo>>(`/chapters/${chapterId}/rewrite/apply`, {
    acceptedChars: 0,
    ...data,
  })
}
