<script setup lang="ts">
/**
 * 命令面板组件。
 *
 * 交互规则：
 * - Cmd/Ctrl+K 打开（由 MainLayout 注册全局监听）。
 * - ESC 关闭，点击遮罩关闭。
 * - ↑↓ 键盘导航，Enter 执行选中命令。
 * - 按 group 分组展示命令列表。
 * - 底部展示当前命令的快捷键提示。
 */
import { ref, computed, watch, nextTick } from 'vue'
import { NInput, NIcon } from 'naive-ui'
import { Search } from 'lucide-vue-next'
import { useCommandPaletteStore } from '@/stores/commandPalette'
import type { PaletteCommand } from '@/stores/commandPalette'

const store = useCommandPaletteStore()

const activeIndex = ref(0)
const inputRef = ref<InstanceType<typeof NInput> | null>(null)

const activeCommand = computed<PaletteCommand | null>(
  () => store.filteredCommands[activeIndex.value] ?? null,
)

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

watch(
  () => store.visible,
  async (v) => {
    if (v) {
      activeIndex.value = 0
      await nextTick()
      const el = (inputRef.value as any)?.$el?.querySelector?.('input') as HTMLElement | null
      el?.focus()
    }
  },
)

watch(() => store.query, () => { activeIndex.value = 0 })

function execute(cmd: PaletteCommand) {
  store.close()
  setTimeout(() => cmd.action(), 80)
}

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
  <Teleport to="body">
    <Transition name="palette-fade">
      <div v-if="store.visible" class="palette-overlay" @click.self="store.close()">
        <div class="palette-container" @keydown="handleKeydown">
          <!-- 搜索框 -->
          <div class="palette-input-wrap">
            <NIcon :component="Search" :size="18" class="palette-search-icon" />
            <NInput
              ref="inputRef"
              v-model:value="store.query"
              placeholder="搜索命令或功能…"
              :bordered="false"
              clearable
              class="palette-input"
            />
          </div>

          <!-- 命令列表 -->
          <div v-if="store.filteredCommands.length" class="palette-list">
            <template v-for="group in groupedCommands" :key="group.name">
              <div class="palette-group-label">{{ group.name }}</div>
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
                <span v-if="cmd.description" class="palette-item-desc">{{ cmd.description }}</span>
                <span v-if="cmd.shortcut" class="palette-item-shortcut">{{ cmd.shortcut }}</span>
              </div>
            </template>
          </div>

          <!-- 空状态 -->
          <div v-else class="palette-empty">
            <span>未找到匹配命令</span>
          </div>

          <!-- 底部快捷键帮助 -->
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
.palette-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 9000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14vh;
}

.palette-container {
  width: 620px;
  max-width: 94vw;
  background: var(--w-bg-elevated);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-lg);
  box-shadow: var(--w-shadow-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 60vh;
}

.palette-input-wrap {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid var(--w-border-subtle);
  gap: 12px;
}

.palette-search-icon {
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

.palette-input {
  font-size: 17px;
  flex: 1;
  background: transparent !important;
}

.palette-input :deep(.n-input__input-el) {
  font-size: 17px !important;
  color: var(--w-text) !important;
}

.palette-list {
  overflow-y: auto;
  flex: 1;
  padding: 8px 0;
}

.palette-group-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--w-text-tertiary);
  padding: 10px 18px 4px;
  user-select: none;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  cursor: pointer;
  border-radius: var(--w-radius-sm);
  margin: 1px 8px;
  transition: all var(--w-transition-fast);
  user-select: none;
}

.palette-item:hover,
.palette-item--active {
  background: var(--w-brand-soft);
}

.palette-item--active {
  border-left: 2px solid var(--w-brand);
  margin-left: 6px;
  padding-left: 16px;
}

.palette-item-icon {
  font-size: 15px;
  width: 22px;
  text-align: center;
  flex-shrink: 0;
  color: var(--w-text-secondary);
}

.palette-item-label {
  font-size: 14px;
  flex: 1;
  font-weight: 500;
  color: var(--w-text);
}

.palette-item-desc {
  font-size: 12px;
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

.palette-item-shortcut {
  font-size: 11px;
  color: var(--w-text-tertiary);
  background: var(--w-bg-tertiary);
  border: 1px solid var(--w-border-default);
  border-radius: 4px;
  padding: 2px 6px;
  font-family: var(--w-font-mono);
  flex-shrink: 0;
}

.palette-empty {
  padding: 40px;
  text-align: center;
  color: var(--w-text-tertiary);
  font-size: 14px;
}

.palette-footer {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 10px 18px;
  border-top: 1px solid var(--w-border-subtle);
  font-size: 11px;
  color: var(--w-text-tertiary);
  flex-wrap: wrap;
  user-select: none;
}

.palette-footer kbd {
  background: var(--w-bg-tertiary);
  border-color: var(--w-border-default);
}

.palette-footer-shortcut {
  margin-left: auto;
  color: var(--w-text-secondary);
  font-size: 12px;
}

/* 过渡动画 */
.palette-fade-enter-active,
.palette-fade-leave-active {
  transition: opacity var(--w-transition-base);
}

.palette-fade-enter-from,
.palette-fade-leave-to {
  opacity: 0;
}

.palette-fade-enter-active .palette-container {
  animation: palette-slide-in var(--w-transition-slow) ease;
}

@keyframes palette-slide-in {
  from { transform: translateY(-12px) scale(0.97); opacity: 0; }
  to   { transform: translateY(0) scale(1); opacity: 1; }
}

@media (max-width: 767px) {
  .palette-overlay {
    padding-top: 6vh;
    align-items: flex-start;
  }

  .palette-container {
    max-height: 80vh;
    border-radius: var(--w-radius-lg) var(--w-radius-lg) 0 0;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    max-width: 100%;
  }
}
</style>
