/**
 * 命令面板 Store（P8-15）。
 *
 * 提供全局命令注册与面板开关管理：
 * - 任意组件可调用 registerCommands() 注册当前上下文命令。
 * - 组件销毁时调用 unregisterCommands() 清理。
 * - MainLayout 监听 Cmd/Ctrl+K 全局快捷键开启面板。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/** 命令条目定义。 */
export interface PaletteCommand {
  /** 唯一 ID（用于注销时精确删除）。 */
  id: string
  /** 显示标签。 */
  label: string
  /** 补充说明（可选）。 */
  description?: string
  /** 快捷键提示文本，如 "Ctrl+F"（可选）。 */
  shortcut?: string
  /** 命令分组，如 "导航" / "写作" / "AI 操作"（可选）。 */
  group?: string
  /** 命令图标（emoji 或 icon key，可选）。 */
  icon?: string
  /** 执行函数。 */
  action: () => void
}

/**
 * 命令面板全局状态。
 */
export const useCommandPaletteStore = defineStore('commandPalette', () => {
  /** 面板是否可见。 */
  const visible = ref(false)
  /** 当前搜索关键词。 */
  const query = ref('')
  /** 已注册的命令列表。 */
  const commands = ref<PaletteCommand[]>([])

  /**
   * 注册一组命令（同 ID 会被替换为新版本）。
   * 推荐在 onMounted 调用，onUnmounted 调用 unregisterCommands。
   */
  function registerCommands(cmds: PaletteCommand[]) {
    const ids = new Set(cmds.map(c => c.id))
    commands.value = [...commands.value.filter(c => !ids.has(c.id)), ...cmds]
  }

  /**
   * 注销一组命令（通过 ID 精确删除）。
   * 在组件 onUnmounted 中调用，防止残留无效命令。
   */
  function unregisterCommands(ids: string[]) {
    const idSet = new Set(ids)
    commands.value = commands.value.filter(c => !idSet.has(c.id))
  }

  /**
   * 根据 query 过滤后的命令列表（大小写不敏感）。
   * 匹配 label 和 description。
   */
  const filteredCommands = computed(() => {
    const q = query.value.trim().toLowerCase()
    if (!q) return commands.value
    return commands.value.filter(
      c =>
        c.label.toLowerCase().includes(q) ||
        c.description?.toLowerCase().includes(q) ||
        c.group?.toLowerCase().includes(q),
    )
  })

  /** 打开面板并清空搜索词。 */
  function open() {
    query.value = ''
    visible.value = true
  }

  /** 关闭面板并清空搜索词。 */
  function close() {
    visible.value = false
    query.value = ''
  }

  /** 切换面板可见状态。 */
  function toggle() {
    if (visible.value) close()
    else open()
  }

  return {
    visible,
    query,
    commands,
    filteredCommands,
    registerCommands,
    unregisterCommands,
    open,
    close,
    toggle,
  }
})
