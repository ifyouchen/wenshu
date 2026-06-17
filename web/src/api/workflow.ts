import client from './client'
import type { ApiResponse, ProjectInfo, ScriptDraftInfo } from './types'
import type { ChapterInfo, OutlineInfo } from './project'
import type { StyleTemplateInfo } from './styleTemplate'

export interface WorkflowTask {
  key: string
  title: string
  description: string
  route: string
  enabled: boolean
}

export interface ChapterBrief {
  id: string
  title: string | null
  volumeTitle: string | null
  wordCount: number
  status: string
}

export interface DashboardState {
  recentProjects: ProjectInfo[]
  continueChapter: ChapterInfo | null
  recentScriptDraft: ScriptDraftInfo | null
  openConsistencyItems: number
  tasks: WorkflowTask[]
}

export interface WriteState {
  project: ProjectInfo
  outline: OutlineInfo
  firstEditableChapter: ChapterInfo | null
  characterCount: number
  worldElementCount: number
  hasSkeletonTask: boolean
}

export interface RewriteState {
  project: ProjectInfo
  outline: OutlineInfo
  chapters: ChapterBrief[]
  styleTemplates: StyleTemplateInfo[]
  importAvailable: boolean
  latestReportId: string | null
}

export interface ScriptState {
  project: ProjectInfo
  outline: OutlineInfo
  drafts: ScriptDraftInfo[]
  latestDraft: ScriptDraftInfo | null
  latestDraftSceneCount: number
  adaptableChapters: ChapterBrief[]
}

export function getDashboard() {
  return client.get<ApiResponse<DashboardState>>('/workflows/dashboard')
}

export function getWriteState(projectId: string) {
  return client.get<ApiResponse<WriteState>>(`/workflows/projects/${projectId}/write-state`)
}

export function getRewriteState(projectId: string) {
  return client.get<ApiResponse<RewriteState>>(`/workflows/projects/${projectId}/rewrite-state`)
}

export function getScriptState(projectId: string) {
  return client.get<ApiResponse<ScriptState>>(`/workflows/projects/${projectId}/script-state`)
}
