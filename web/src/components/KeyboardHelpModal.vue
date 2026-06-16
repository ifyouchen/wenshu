<script setup lang="ts">
/**
 * 快捷键参考面板（P8-21）。
 *
 * 触发方式：
 * - `?` 键（由 MainLayout 全局监听）
 * - 命令面板中的"快捷键参考"命令
 *
 * 覆盖范围：
 * - 全局导航：Ctrl+K / ESC 等
 * - 章节编辑：Ctrl+F / Ctrl+H / Ctrl+S
 * - AI 操作：选中文字 / AI 浮窗
 * - 视图切换：快照历史 / 移动端侧栏
 */
import { NModal, NScrollbar, NIcon } from 'naive-ui'
import { Command, Keyboard } from 'lucide-vue-next'
import { useKeyboardHelpStore, SHORTCUT_GROUPS } from '@/stores/keyboardHelp'
import type { ShortcutItem } from '@/stores/keyboardHelp'

const store = useKeyboardHelpStore()

function renderKeys(item: ShortcutItem): string[] {
  return item.keys
}
</script>

<template>
  <Teleport to="body">
    <NModal
      v-model:show="store.visible"
      preset="card"
      title="快捷键参考"
      style="width: min(640px, 96vw)"
      :segmented="{ content: true }"
    >
      <template #header>
        <div class="modal-title">
          <NIcon :component="Keyboard" :size="18" class="modal-title-icon" />
          <span>快捷键参考</span>
        </div>
      </template>

      <NScrollbar style="max-height: 70vh">
        <div class="kbd-help-body">
          <div
            v-for="group in SHORTCUT_GROUPS"
            :key="group.title"
            class="kbd-group"
          >
            <div class="kbd-group-title">
              <NIcon :component="group.icon" :size="14" class="group-icon" />
              <span>{{ group.title }}</span>
            </div>

            <table class="kbd-table">
              <tbody>
                <tr v-for="(item, i) in group.shortcuts" :key="i" class="kbd-row">
                  <td class="kbd-desc">{{ item.description }}</td>
                  <td class="kbd-keys">
                    <span
                      v-for="(k, ki) in renderKeys(item)"
                      :key="ki"
                      class="kbd-key-group"
                    >
                      <kbd>{{ k }}</kbd>
                      <span
                        v-if="ki < renderKeys(item).length - 1"
                        class="kbd-plus"
                      >+</span>
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="kbd-footer">
            <NIcon :component="Command" :size="12" />
            <span>按 <kbd>?</kbd> 或通过命令面板（<kbd>Ctrl</kbd>+<kbd>K</kbd>）快速打开此面板</span>
          </div>
        </div>
      </NScrollbar>
    </NModal>
  </Teleport>
</template>

<style scoped>
.modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-title-icon {
  color: var(--w-brand);
}

.kbd-help-body {
  padding: 4px 0 8px;
}

.kbd-group {
  margin-bottom: 24px;
}

.kbd-group:last-of-type {
  margin-bottom: 0;
}

.kbd-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--w-text-tertiary);
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--w-border-subtle);
}

.group-icon {
  color: var(--w-brand);
}

.kbd-table {
  width: 100%;
  border-collapse: collapse;
}

.kbd-row {
  border-bottom: 1px solid var(--w-border-subtle);
}

.kbd-row:last-child {
  border-bottom: none;
}

.kbd-desc {
  padding: 8px 12px 8px 4px;
  font-size: 14px;
  line-height: 1.5;
  width: 100%;
  color: var(--w-text);
}

.kbd-keys {
  padding: 8px 0;
  text-align: right;
  white-space: nowrap;
}

.kbd-key-group {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.kbd-plus {
  font-size: 11px;
  color: var(--w-text-muted);
  margin: 0 1px;
}

kbd {
  display: inline-block;
  padding: 2px 7px;
  border: 1px solid var(--w-border-strong);
  border-bottom-width: 2px;
  border-radius: 5px;
  font-family: var(--w-font-mono);
  font-size: 12px;
  background: var(--w-bg-tertiary);
  color: var(--w-text);
  line-height: 1.6;
}

.kbd-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  padding: 10px 12px;
  background: var(--w-bg-tertiary);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  font-size: 12px;
  color: var(--w-text-tertiary);
  text-align: center;
  line-height: 1.6;
}

@media (max-width: 767px) {
  .kbd-desc { font-size: 13px; }
  .kbd-keys { font-size: 11px; }
}
</style>
