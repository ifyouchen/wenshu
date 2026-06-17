/** 角色库与世界观词典 API（P8-07）。 */
import client from './client'
import type { ApiResponse } from './types'

export interface CharacterInfo {
  id: string
  projectId: string
  name: string
  role: string | null
  gender?: string | null
  age?: string | null
  appearance: string | null
  personality: string | null
  abilities: string
  background?: string | null
  motivation?: string | null
  relationships?: string | null
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

export interface CharacterPayload {
  name: string
  role?: string
  gender?: string
  age?: string
  appearance?: string
  personality?: string
  abilities?: string
  background?: string
  motivation?: string
  relationships?: string
  speechStyle?: string
  status?: string
}

export interface WorldElementPayload {
  type: string
  name: string
  description?: string
  aliases?: string[]
}

/** 查询作品角色列表。 */
export function listCharacters(projectId: string) {
  return client.get<ApiResponse<CharacterInfo[]>>(`/projects/${projectId}/characters`)
}

/** 创建角色。 */
export function createCharacter(projectId: string, data: CharacterPayload) {
  return client.post<ApiResponse<CharacterInfo>>(`/projects/${projectId}/characters`, data)
}

/** 更新角色。 */
export function updateCharacter(characterId: string, data: Partial<CharacterPayload>) {
  return client.put<ApiResponse<CharacterInfo>>(`/characters/${characterId}`, data)
}

/** 删除角色。 */
export function deleteCharacter(characterId: string) {
  return client.delete<ApiResponse<void>>(`/characters/${characterId}`)
}

/** 切换角色锁定状态。 */
export function toggleCharacterLock(characterId: string) {
  return client.put<ApiResponse<CharacterInfo>>(`/characters/${characterId}/lock`)
}

/** 查询世界观词典列表。 */
export function listWorldElements(projectId: string) {
  return client.get<ApiResponse<WorldElementInfo[]>>(`/projects/${projectId}/world-dict`)
}

/** 创建世界观/词典条目。 */
export function createWorldElement(projectId: string, data: WorldElementPayload) {
  return client.post<ApiResponse<WorldElementInfo>>(`/projects/${projectId}/world-dict`, data)
}

/** 更新世界观/词典条目。 */
export function updateWorldElement(elementId: string, data: Partial<WorldElementPayload>) {
  return client.put<ApiResponse<WorldElementInfo>>(`/world-dict/${elementId}`, data)
}

/** 删除世界观/词典条目。 */
export function deleteWorldElement(elementId: string) {
  return client.delete<ApiResponse<void>>(`/world-dict/${elementId}`)
}
