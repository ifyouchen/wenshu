/**
 * useDevice — 设备类型检测（P8-14）。
 *
 * 提供响应式的设备判断：
 * - isMobile：宽度 < 768px（手机 / 小平板竖屏）
 * - isTablet：768px ≤ 宽度 < 1024px
 * - isDesktop：宽度 ≥ 1024px
 * - toastPlacement：移动端返回 'top'，桌面端返回 'bottom-right'
 */
import { computed } from 'vue'
import { useWindowSize } from '@vueuse/core'

/** 移动端断点（px）。 */
export const MOBILE_BREAKPOINT = 768
/** 平板断点（px）。 */
export const TABLET_BREAKPOINT = 1024

/**
 * 设备检测 composable。
 *
 * @example
 * const { isMobile, isDesktop, toastPlacement } = useDevice()
 */
export function useDevice() {
  const { width } = useWindowSize()

  /** 是否为移动端（< 768px）。 */
  const isMobile = computed(() => width.value < MOBILE_BREAKPOINT)

  /** 是否为平板端（768px ~ 1023px）。 */
  const isTablet = computed(
    () => width.value >= MOBILE_BREAKPOINT && width.value < TABLET_BREAKPOINT,
  )

  /** 是否为桌面端（≥ 1024px）。 */
  const isDesktop = computed(() => width.value >= TABLET_BREAKPOINT)

  /**
   * Toast 弹出位置。
   * 移动端：顶部居中；桌面端：右下角堆叠。
   */
  const toastPlacement = computed(() =>
    isMobile.value ? 'top' : 'bottom-right',
  )

  return { isMobile, isTablet, isDesktop, toastPlacement, width }
}
