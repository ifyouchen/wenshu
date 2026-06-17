import client, { getAccessToken } from './client'
import type { ApiResponse } from './types'

export interface BranchSuggestion {
  id: number
  direction: string
  summary: string
}

export function createBranch(chapterId: string, branchCount = 3) {
  return client.post<ApiResponse<BranchSuggestion[]>>('/novel/branch', { chapterId, branchCount })
}

export async function continueNovel(chapterId: string, onToken: (text: string) => void) {
  const token = getAccessToken()
  const res = await fetch(`/api/v1/novel/continue?chapterId=${encodeURIComponent(chapterId)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  })
  if (!res.ok || !res.body) throw new Error('续写请求失败')

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    chunk
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.replace(/^data:\s*/, ''))
      .filter((line) => line && line !== '[DONE]')
      .forEach(onToken)
  }
}
