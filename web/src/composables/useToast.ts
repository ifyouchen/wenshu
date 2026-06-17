import { reactive } from 'vue'

export type ToastType = 'success' | 'warning' | 'error' | 'info'

export interface ToastItem {
  id: number
  type: ToastType
  content: string
}

const toasts = reactive<ToastItem[]>([])
let nextId = 1

function push(type: ToastType, content: string, duration = 3000) {
  const id = nextId++
  toasts.push({ id, type, content })
  window.setTimeout(() => dismiss(id), duration)
}

function dismiss(id: number) {
  const index = toasts.findIndex((item) => item.id === id)
  if (index !== -1) toasts.splice(index, 1)
}

export const globalToast = {
  toasts,
  dismiss,
  success: (content: string) => push('success', content),
  warning: (content: string) => push('warning', content),
  error: (content: string) => push('error', content, 4200),
  info: (content: string) => push('info', content),
  loading: (content: string) => {
    const id = nextId++
    toasts.push({ id, type: 'info', content })
    return { destroy: () => dismiss(id) }
  },
}

export function useToast() {
  return globalToast
}
