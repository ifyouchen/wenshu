/** 润色建议 API。 */
import client from './client'
import type { ApiResponse } from './types'

export interface PolishAnnotation {
  original: string
  suggested: string
  reason: string
}

export interface PolishResult {
  mode: string
  basicAnnotations: PolishAnnotation[] | null
  rewritten: string | null
}

export function polishBasic(text: string) {
  return client.post<ApiResponse<PolishResult>>('/polish/basic', { text })
}

export function polishAdvanced(text: string, instruction?: string) {
  return client.post<ApiResponse<PolishResult>>('/polish/advanced', { text, instruction })
}

export function polishStyle(text: string, styleDescription: string) {
  return client.post<ApiResponse<PolishResult>>('/polish/style', { text, styleDescription })
}
