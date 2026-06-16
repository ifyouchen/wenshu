/**
 * useUpgradeModal — 订阅升级引导弹窗状态（P8-19）。
 *
 * 三个触发场景：
 * - quota-chars：AI 字符配额用尽（来自 C 级错误状态）
 * - quota-adaptations：改编/审查次数用尽
 * - pro-feature：用户点击了专业版专属功能
 *
 * 在 MainLayout 中挂载 UpgradeModal，任意组件均可调用此 composable 触发弹窗。
 */
import { ref } from 'vue'

/** 升级引导触发场景。 */
export type UpgradeScenario = 'quota-chars' | 'quota-adaptations' | 'pro-feature'

/** 弹窗可见状态。 */
const visible = ref(false)

/** 当前触发场景。 */
const scenario = ref<UpgradeScenario>('pro-feature')

/**
 * 打开升级弹窗。
 *
 * @param s 触发场景
 * @example
 * const { openUpgrade } = useUpgradeModal()
 * openUpgrade('quota-chars')
 */
function openUpgrade(s: UpgradeScenario = 'pro-feature') {
  scenario.value = s
  visible.value = true
}

/** 关闭升级弹窗。 */
function closeUpgrade() {
  visible.value = false
}

/**
 * useUpgradeModal composable。
 * 每次调用返回同一全局状态（模块级单例）。
 */
export function useUpgradeModal() {
  return { visible, scenario, openUpgrade, closeUpgrade }
}
