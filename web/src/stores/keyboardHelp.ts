/**
 * 快捷键参考面板 Store（P8-21）。
 *
 * 提供面板开关状态，MainLayout 和 EditorView 均可调用 open() 打开面板。
 */
import { defineStore } from 'pinia'
import { ref, type Component } from 'vue'
import {
  Compass,
  Pencil,
  Wand2,
  LayoutGrid,
} from 'lucide-vue-next'

/**
 * 快捷键条目定义。
 */
export interface ShortcutItem {
  /** 快捷键显示文本，如 "Ctrl+K"，多键用 "+" 分隔。 */
  keys: string[]
  /** 动作描述。 */
  description: string
}

/**
 * 快捷键分组定义。
 */
export interface ShortcutGroup {
  /** 分组标题，如 "导航" / "写作" / "创作辅助"。 */
  title: string
  /** 分组图标（Lucide 组件）。 */
  icon: Component
  /** 该分组下的快捷键列表。 */
  shortcuts: ShortcutItem[]
}

/**
 * 应用快捷键参考数据。
 */
export const SHORTCUT_GROUPS: ShortcutGroup[] = [
  {
    title: '全局导航',
    icon: Compass,
    shortcuts: [
      { keys: ['Ctrl', 'K'], description: '打开命令面板（快速跳转 / 执行命令）' },
      { keys: ['?'], description: '打开快捷键参考面板' },
      { keys: ['Esc'], description: '关闭弹层 / 取消当前操作' },
    ],
  },
  {
    title: '章节编辑',
    icon: Pencil,
    shortcuts: [
      { keys: ['Ctrl', 'S'], description: '保存剧本场景内容（剧本工作台）' },
      { keys: ['Ctrl', 'F'], description: '打开全书搜索横条' },
      { keys: ['Ctrl', 'H'], description: '打开全书搜索替换横条' },
    ],
  },
  {
    title: '创作辅助',
    icon: Wand2,
    shortcuts: [
      { keys: ['选中文字'], description: '唤起辅助浮窗，对选中内容进行续写或润色' },
      { keys: ['Esc'], description: '取消流式续写' },
    ],
  },
  {
    title: '视图与导航',
    icon: LayoutGrid,
    shortcuts: [
      { keys: ['↑', '↓'], description: '命令面板中上下导航命令' },
      { keys: ['Enter'], description: '命令面板中执行选中命令' },
      { keys: ['历史'], description: '标题栏点击：打开版本快照与 diff' },
      { keys: ['侧栏按钮'], description: '移动端：打开大纲 / 角色库侧栏' },
    ],
  },
]

/**
 * 快捷键面板全局状态。
 */
export const useKeyboardHelpStore = defineStore('keyboardHelp', () => {
  /** 面板是否可见。 */
  const visible = ref(false)

  /** 打开面板。 */
  function open() { visible.value = true }

  /** 关闭面板。 */
  function close() { visible.value = false }

  /** 切换面板。 */
  function toggle() { visible.value = !visible.value }

  return { visible, open, close, toggle }
})
