/**
 * 订阅套餐 API（P9-01/P9-02）。
 */
import client from './client'
import type { ApiResponse, QuotaInfo } from './types'

/** 订阅套餐摘要。 */
export interface PlanInfo {
  planKey: string
  name: string
  monthlyCharLimit: number
  monthlyAdaptationLimit: number
  pricePerMonth: number
  description: string
}

/** 当前订阅及配额详情。 */
export interface CurrentSubscriptionInfo {
  planKey: string
  planName: string
  status: string
  expiresAt: string | null
  quota: QuotaInfo
}

/** 查询所有有效套餐列表（无需鉴权）。 */
export function getSubscriptionPlans() {
  return client.get<ApiResponse<PlanInfo[]>>('/subscriptions/plans')
}

/** 获取当前用户订阅及配额详情（需鉴权）。 */
export function getCurrentSubscription() {
  return client.get<ApiResponse<CurrentSubscriptionInfo>>('/subscriptions/current')
}
