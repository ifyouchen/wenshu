/**
 * useToast — 全局 Toast 通知系统（P8-16）。
 *
 * 特性：
 * - 桌面端：右下角堆叠展示
 * - 移动端：顶部居中展示（通过 NMessageProvider placement 动态切换）
 * - 四类通知：success / warning / error / info
 * - 自动消失（可配置 duration）
 * - 支持手动关闭
 * - 全局可调用：组件外（API 拦截器等）可使用 globalToast 单例
 */
import { ref } from 'vue'
import type { MessageApi, MessageOptions } from 'naive-ui'

/** 全局 MessageApi 引用（在 App.vue 中通过 useMessage() 初始化）。 */
const _messageApi = ref<MessageApi | null>(null)

/**
 * 初始化全局 toast 引用。
 * 必须在 NMessageProvider 内的组件中调用一次（App.vue setup 或 MainLayout setup）。
 */
export function initGlobalToast(api: MessageApi) {
  _messageApi.value = api
}

/** 默认展示时长（毫秒）。 */
const DEFAULT_DURATION = 3000

/**
 * globalToast：全局 Toast 单例，可在任意位置（包括 API 拦截器）调用。
 *
 * @example
 * import { globalToast } from '@/composables/useToast'
 * globalToast.success('保存成功')
 * globalToast.error('网络错误', { duration: 5000 })
 */
export const globalToast = {
  /** 成功通知。 */
  success(content: string, options?: MessageOptions) {
    _messageApi.value?.success(content, { duration: DEFAULT_DURATION, ...options })
  },
  /** 警告通知。 */
  warning(content: string, options?: MessageOptions) {
    _messageApi.value?.warning(content, { duration: DEFAULT_DURATION, ...options })
  },
  /** 错误通知。 */
  error(content: string, options?: MessageOptions) {
    _messageApi.value?.error(content, { duration: 4000, ...options })
  },
  /** 信息通知。 */
  info(content: string, options?: MessageOptions) {
    _messageApi.value?.info(content, { duration: DEFAULT_DURATION, ...options })
  },
  /** 加载通知（返回 destroy 函数以手动关闭）。 */
  loading(content: string, options?: MessageOptions) {
    return _messageApi.value?.loading(content, { duration: 0, ...options })
  },
}

/**
 * useToast — 在组件内使用时的快捷封装。
 * 与 globalToast 功能相同，直接转发到 globalToast 单例。
 *
 * @example
 * const toast = useToast()
 * toast.success('操作成功')
 */
export function useToast() {
  return globalToast
}
