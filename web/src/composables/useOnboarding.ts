/**
 * useOnboarding — 渐进式用户引导状态管理（P8-18）。
 *
 * 所有引导提示只展示一次：已展示的提示 ID 持久化到 localStorage，
 * 刷新或重新登录后不再重复打扰。
 *
 * 引导触发场景：
 * - first-home：首次进入首页（无作品时展示欢迎引导）
 * - first-editor：首次进入编辑器（展示基础写作提示）
 * - first-ai-select：首次选中文字（展示创作辅助使用说明）
 * - char-milestone-3000：写作累计超过 3000 字里程碑（显示鼓励提示）
 * - first-snapshot：首次创建快照（展示版本历史说明）
 *
 * 使用方式：
 * ```ts
 * const ob = useOnboarding()
 * ob.isDone('first-home')         // 查询是否已展示
 * ob.markDone('first-home')       // 标记已展示
 * ob.shouldShow('first-home')     // isDone 的反义
 * ```
 */

/** localStorage 存储 key。 */
const STORAGE_KEY = 'wenshu:onboarding:seen'

/** 所有引导 ID 类型定义（增加新场景时在此扩展）。 */
export type OnboardingId =
  | 'first-home'
  | 'first-editor'
  | 'first-ai-select'
  | 'char-milestone-3000'
  | 'first-snapshot'

/**
 * 从 localStorage 读取已展示集合。
 * localStorage 存储为逗号分隔的 ID 字符串。
 */
function readSeen(): Set<string> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return new Set()
    return new Set(raw.split(',').filter(Boolean))
  } catch {
    return new Set()
  }
}

/**
 * 将已展示集合写入 localStorage。
 */
function writeSeen(seen: Set<string>): void {
  try {
    localStorage.setItem(STORAGE_KEY, Array.from(seen).join(','))
  } catch {
    // localStorage 不可用时静默降级
  }
}

/**
 * useOnboarding — 引导状态查询与标记。
 *
 * 注意：每次调用 useOnboarding() 都会读取最新 localStorage 状态，
 * 无需在组件间共享实例。
 */
export function useOnboarding() {
  /**
   * 检查指定引导是否已展示过。
   * @param id 引导 ID
   */
  function isDone(id: OnboardingId): boolean {
    return readSeen().has(id)
  }

  /**
   * isDone 的反义，用于 v-if 条件判断更语义化。
   * @param id 引导 ID
   */
  function shouldShow(id: OnboardingId): boolean {
    return !isDone(id)
  }

  /**
   * 标记指定引导为已展示（用户已看到或手动关闭后调用）。
   * @param id 引导 ID
   */
  function markDone(id: OnboardingId): void {
    const seen = readSeen()
    seen.add(id)
    writeSeen(seen)
  }

  /**
   * 重置所有引导状态（仅在开发/测试中使用）。
   */
  function resetAll(): void {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // 静默降级
    }
  }

  return { isDone, shouldShow, markDone, resetAll }
}
