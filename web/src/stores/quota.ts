/**
 * 配额 Store（P8-03）。
 * 全局单例，创作辅助操作完成后调用 refresh() 更新显示；
 * 数据最多允许 60 秒延迟（前端 Tooltip 提示："配额用量每次操作完成后更新"）。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getQuota } from '@/api/user'
import type { QuotaInfo } from '@/api/types'

export const useQuotaStore = defineStore('quota', () => {
  /** 当前月度配额详情，null 表示尚未加载。 */
  const quota = ref<QuotaInfo | null>(null)
  /** 是否正在加载。 */
  const loading = ref(false)
  /** 最后一次刷新时间戳（ms）。 */
  const lastRefreshAt = ref(0)

  /** 创作辅助字数剩余百分比（0~100）。 */
  const charUsagePercent = computed(() => {
    if (!quota.value || quota.value.limitChars === 0) return 0
    return Math.min(100, Math.round((quota.value.usedChars / quota.value.limitChars) * 100))
  })

  /** 改编次数剩余百分比（0~100）。 */
  const adaptationUsagePercent = computed(() => {
    if (!quota.value || quota.value.limitAdaptations === 0) return 0
    return Math.min(
      100,
      Math.round((quota.value.usedAdaptations / quota.value.limitAdaptations) * 100),
    )
  })

  /**
   * 从后端刷新配额数据。
   * 60 秒内重复调用会使用缓存值，避免频繁请求。
   *
   * @param force 强制刷新，忽略 60 秒缓存
   */
  async function refresh(force = false): Promise<void> {
    const now = Date.now()
    if (!force && quota.value && now - lastRefreshAt.value < 60_000) return
    loading.value = true
    try {
      const res = await getQuota()
      quota.value = res.data.data
      lastRefreshAt.value = now
    } catch {
      // 静默失败，保留上次数据
    } finally {
      loading.value = false
    }
  }

  return { quota, loading, charUsagePercent, adaptationUsagePercent, refresh }
})
