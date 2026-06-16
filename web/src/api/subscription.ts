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

/** 支付订单摘要（P9-03）。 */
export interface OrderInfo {
  id: string
  orderNo: string
  productType: string
  productKey: string
  amountFen: number
  currency: string
  status: string
  paidAt: string | null
  createdAt: string
  /** 支付跳转 URL（pending 状态时有效）。 */
  payUrl: string
}

/** 查询所有有效套餐列表（无需鉴权）。 */
export function getSubscriptionPlans() {
  return client.get<ApiResponse<PlanInfo[]>>('/subscriptions/plans')
}

/** 获取当前用户订阅及配额详情（需鉴权）。 */
export function getCurrentSubscription() {
  return client.get<ApiResponse<CurrentSubscriptionInfo>>('/subscriptions/current')
}

/** 创建订阅购买订单（P9-03）。 */
export function createCheckout(planKey: string, channel?: string) {
  return client.post<ApiResponse<OrderInfo>>('/subscriptions/checkout', { planKey, channel })
}

/** 创建字数包充值订单（P9-03）。 */
export function createTopup(topupKey: string, channel?: string) {
  return client.post<ApiResponse<OrderInfo>>('/subscriptions/topup', { topupKey, channel })
}

/** 取消自动续费（P9-03）。 */
export function cancelSubscription() {
  return client.post<ApiResponse<{ status: string }>>('/subscriptions/cancel')
}
