/** 角色库与世界观词典 API（P8-07）。 */
import client from './client'
import type { ApiResponse } from './types'

export interface CharacterInfo {
  id: string
  projectId: string
  name: string
  role: string | null
  appearance: string | null
  personality: string | null
  abilities: string
  speechStyle: string | null
  status: string
  locked: boolean
  createdAt: string
  updatedAt: string
}

export interface WorldElementInfo {
  id: string
  projectId: string
  type: string | null
  name: string | null
  description: string | null
  aliases: string[]
  locked: boolean
  createdAt: string
}

/** 查询作品角色列表。 */
export function listCharacters(projectId: string) {
  return client.get<ApiResponse<CharacterInfo[]>>(`/projects/${projectId}/characters`)
}

/** 切换角色锁定状态。 */
export function toggleCharacterLock(characterId: string) {
  return client.put<ApiResponse<CharacterInfo>>(`/characters/${characterId}/lock`)
}

/** 查询世界观词典列表。 */
export function listWorldElements(projectId: string) {
  return client.get<ApiResponse<WorldElementInfo[]>>(`/projects/${projectId}/world-dict`)
}
