import client from './client'
import type { ApiResponse } from './types'

export interface StoryToolInfo {
  id: string
  name: string
  description: string
  modelLane: 'creative' | 'utility'
}

export interface StoryToolRequest {
  projectId?: string
  chapterId?: string
  input?: string
  instruction?: string
  targetWords?: number
}

export interface StoryToolResult {
  tool: string
  projectId: string | null
  chapterId: string | null
  output: string
}

export function listStoryTools() {
  return client.get<ApiResponse<StoryToolInfo[]>>('/story-tools')
}

export function runStoryTool(tool: string, payload: StoryToolRequest) {
  return client.post<ApiResponse<StoryToolResult>>(`/story-tools/${tool}/run`, payload)
}
