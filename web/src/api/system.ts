import client from './client'
import type { ApiResponse } from './types'

export interface SystemHealth {
  status: string
  product: string
  apiVersion: string
  time: string
}

export function getSystemHealth() {
  return client.get<ApiResponse<SystemHealth>>('/system/health')
}
