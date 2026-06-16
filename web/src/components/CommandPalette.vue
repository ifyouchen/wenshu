<script setup lang="ts">
/**
 * 命令面板组件（P8-15）。
 *
 * 交互规则：
 * - Cmd/Ctrl+K 打开（由 MainLayout 注册全局监听）。
 * - ESC 关闭，点击遮罩关闭。
 * - ↑↓ 键盘导航，Enter 执行选中命令。
 * - 按 group 分组展示命令列表。
 * - 底部展示当前命令的快捷键提示。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { NInput } from 'naive-ui'
import { useCommandPaletteStore } from '@/stores/commandPalette'
import type { PaletteCommand } from '@/stores/commandPalette'

const store = useCommandPaletteStore()

/** 当前键盘高亮的命令索引。 */
const activeIndex = ref(0)
/** 搜索框 ref，用于自动聚焦。 */
const inputRef = ref<InstanceType<typeof NInput> | null>(null)

/** 当前激活的命令对象（如果有）。 */
const activeCommand = computed<PaletteCommand | null>(
  () => store.filteredCommands[activeIndex.value] ?? null,
)

/** 将命令列表按 group 分组，保持原始顺序。 */
const groupedCommands = computed(() => {
  const groups: { name: string; commands: (PaletteCommand & { flatIndex: number })[] }[] = []
  const groupMap = new Map<string, typeof groups[0]>()
  store.filteredCommands.forEach((cmd, idx) => {
    const g = cmd.group || '操作'
    if (!groupMap.has(g)) {
      const grp = { name: g, commands: [] as (PaletteCommand & { flatIndex: number })[] }
      groupMap.set(g, grp)
      groups.push(grp)
    }
    groupMap.get(g)!.commands.push({ ...cmd, flatIndex: idx })
  })
  return groups
})

// 打开时重置高亮 + 聚焦输入框
watch(
  () => store.visible,
  async (v) => {
    if (v) {
      activeIndex.value = 0
      await nextTick()
      // 尝试聚焦 NInput 内部的 input 元素
      const el = (inputRef.value as any)?.$el?.querySelector?.('input') as HTMLElement | null
      el?.focus()
    }
  },
)

// 搜索词变化时重置高亮
watch(() => store.query, () => { activeIndex.value = 0 })

/** 执行命令并关闭面板。 */
function execute(cmd: PaletteCommand) {
  store.close()
  // 用 setTimeout 确保面板关闭动画完成后执行，避免焦点冲突
  setTimeout(() => cmd.action(), 80)
}

/** 键盘事件处理（上下导航 / 回车执行 / ESC 关闭）。 */
function handleKeydown(e: KeyboardEvent) {
  const total = store.filteredCommands.length
  if (total === 0) {
    if (e.key === 'Escape') store.close()
    return
  }
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % total
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + total) % total
  } else if (e.key === 'Enter') {
    e.preventDefault()
    const cmd = store.filteredCommands[activeIndex.value]
    if (cmd) execute(cmd)
  } else if (e.key === 'Escape') {
    store.close()
  }
}
</script>

<template>
  <!-- 遮罩层（可见时显示） -->
  <Teleport to="body">
    <Transition name="palette-fade">
      <div v-if="store.visible" class="palette-overlay" @click.self="store.close()">
        <div class="palette-container" @keydown="handleKeydown">
          <!-- 搜索框 -->
          <div class="palette-input-wrap">
            <span class="palette-search-icon">🔍</span>
            <NInput
              ref="inputRef"
              v-model:value="store.query"
              placeholder="搜索命令…"
              :bordered="false"
              clearable
              class="palette-input"
            />
          </div>

          <!-- 命令列表 -->
          <div class="palette-list" v-if="store.filteredCommands.length">
            <template v-for="group in groupedCommands" :key="group.name">
              <!-- 分组标签 -->
              <div class="palette-group-label">{{ group.name }}</div>
              <!-- 分组命令 -->
              <div
                v-for="cmd in group.commands"
                :key="cmd.id"
                class="palette-item"
                :class="{ 'palette-item--active': cmd.flatIndex === activeIndex }"
                @click="execute(cmd)"
                @mouseenter="activeIndex = cmd.flatIndex"
              >
                <span class="palette-item-icon" v-if="cmd.icon">{{ cmd.icon }}</span>
                <span class="palette-item-label">{{ cmd.label }}</span>
                <span class="palette-item-desc" v-if="cmd.description">{{ cmd.description }}</span>
                <span class="palette-item-shortcut" v-if="cmd.shortcut">{{ cmd.shortcut }}</span>
              </div>
            </template>
          </div>

          <!-- 空状态 -->
          <div class="palette-empty" v-else>
            <span>未找到匹配命令</span>
          </div>

          <!-- 底部快捷键帮助提示 -->
          <div class="palette-footer">
            <span><kbd>↑</kbd><kbd>↓</kbd> 导航</span>
            <span><kbd>↵</kbd> 执行</span>
            <span><kbd>ESC</kbd> 关闭</span>
            <span v-if="activeCommand?.shortcut" class="palette-footer-shortcut">
              当前命令快捷键：<kbd>{{ activeCommand.shortcut }}</kbd>
            </span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ─── 遮罩 ─── */
.palette-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14vh;
}

/* ─── 面板容器 ─── */
.palette-container {
  width: 580px;
  max-width: 94vw;
  background: var(--n-color, #fff);
  border-radius: 12px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.22);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 60vh;
}

/* ─── 搜索框区域 ─── */
.palette-input-wrap {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(128, 128, 128, 0.15);
  gap: 8px;
}
.palette-search-icon { font-size: 15px; opacity: 0.5; flex-shrink: 0; }
.palette-input { font-size: 16px; flex: 1; }
:deep(.n-input__input-el) { font-size: 16px !important; }

/* ─── 命令列表 ─── */
.palette-list {
  overflow-y: auto;
  flex: 1;
  padding: 6px 0;
}

/* ─── 分组标签 ─── */
.palette-group-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  opacity: 0.45;
  padding: 10px 16px 4px;
  user-select: none;
}

/* ─── 命令条目 ─── */
.palette-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin: 1px 8px;
  transition: background 0.1s;
  user-select: none;
}
.palette-item:hover,
.palette-item--active {
  background: rgba(99, 102, 241, 0.1);
}
.palette-item-icon { font-size: 16px; width: 20px; text-align: center; flex-shrink: 0; }
.palette-item-label { font-size: 14px; flex: 1; font-weight: 500; }
.palette-item-desc { font-size: 12px; opacity: 0.5; }
.palette-item-shortcut {
  font-size: 11px;
  opacity: 0.55;
  background: rgba(128, 128, 128, 0.12);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: ui-monospace, monospace;
  flex-shrink: 0;
}

/* ─── 空状态 ─── */
.palette-empty {
  padding: 32px;
  text-align: center;
  opacity: 0.4;
  font-size: 14px;
}

/* ─── 底部快捷键帮助 ─── */
.palette-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  border-top: 1px solid rgba(128, 128, 128, 0.15);
  font-size: 11px;
  opacity: 0.5;
  flex-wrap: wrap;
  user-select: none;
}
.palette-footer-shortcut {
  margin-left: auto;
  opacity: 0.8;
  font-size: 12px;
}
kbd {
  display: inline-block;
  padding: 1px 5px;
  border: 1px solid rgba(128, 128, 128, 0.3);
  border-radius: 3px;
  font-family: ui-monospace, monospace;
  font-size: 11px;
  background: rgba(128, 128, 128, 0.08);
  margin: 0 1px;
}

/* ─── 过渡动画 ─── */
.palette-fade-enter-active,
.palette-fade-leave-active {
  transition: opacity 0.15s ease;
}
.palette-fade-enter-from,
.palette-fade-leave-to {
  opacity: 0;
}
.palette-fade-enter-active .palette-container {
  animation: palette-slide-in 0.15s ease;
}
@keyframes palette-slide-in {
  from { transform: translateY(-12px) scale(0.97); opacity: 0; }
  to   { transform: translateY(0) scale(1); opacity: 1; }
}

/* ─── 移动端适配 ─── */
@media (max-width: 767px) {
  .palette-overlay {
    padding-top: 5vh;
    align-items: flex-start;
  }
  .palette-container {
    max-height: 80vh;
    border-radius: 12px 12px 0 0;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    max-width: 100%;
  }
}
</style>
