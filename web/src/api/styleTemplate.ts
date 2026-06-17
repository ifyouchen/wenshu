import client from './client'
import type { ApiResponse } from './types'

export interface StyleTemplateInfo {
  id: string
  userId?: string
  name: string
  type: string
  description: string | null
  sampleText?: string | null
  prompt?: string | null
  active: boolean
  createdAt?: string
  updatedAt?: string
}

export interface StyleTemplatePayload {
  name: string
  type: string
  description?: string
  sampleText?: string
  prompt?: string
}

export function listStyleTemplates(type?: string) {
  const query = type ? `?type=${encodeURIComponent(type)}` : ''
  return client.get<ApiResponse<StyleTemplateInfo[]>>(`/style-templates${query}`)
}

export function createStyleTemplate(data: StyleTemplatePayload) {
  return client.post<ApiResponse<StyleTemplateInfo>>('/style-templates', data)
}

export function updateStyleTemplate(id: string, data: Partial<StyleTemplatePayload>) {
  return client.put<ApiResponse<StyleTemplateInfo>>(`/style-templates/${id}`, data)
}

export function activateStyleTemplate(id: string) {
  return client.put<ApiResponse<StyleTemplateInfo>>(`/style-templates/${id}/activate`)
}

export function deleteStyleTemplate(id: string) {
  return client.delete<ApiResponse<void>>(`/style-templates/${id}`)
}
