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
import { NModal, NScrollbar } from 'naive-ui'
import { useKeyboardHelpStore, SHORTCUT_GROUPS } from '@/stores/keyboardHelp'
import type { ShortcutItem } from '@/stores/keyboardHelp'

const store = useKeyboardHelpStore()

/**
 * 将快捷键 keys 数组渲染为展示友好的字符串。
 * 单个 key 直接返回，多个 key 之间自动以 "+" 连接。
 */
function renderKeys(item: ShortcutItem): string[] {
  return item.keys
}
</script>

<template>
  <!-- 快捷键参考弹窗（Teleport 避免层叠遮挡问题） -->
  <Teleport to="body">
    <NModal
      v-model:show="store.visible"
      preset="card"
      title="⌨️ 快捷键参考"
      style="width: min(640px, 96vw)"
      :segmented="{ content: true }"
    >
      <NScrollbar style="max-height: 70vh">
        <div class="kbd-help-body">
          <!-- 遍历所有快捷键分组 -->
          <div
            v-for="group in SHORTCUT_GROUPS"
            :key="group.title"
            class="kbd-group"
          >
            <!-- 分组标题 -->
            <div class="kbd-group-title">
              <span>{{ group.icon }}</span>
              <span>{{ group.title }}</span>
            </div>

            <!-- 快捷键列表 -->
            <table class="kbd-table">
              <tbody>
                <tr v-for="(item, i) in group.shortcuts" :key="i" class="kbd-row">
                  <!-- 操作说明 -->
                  <td class="kbd-desc">{{ item.description }}</td>
                  <!-- 快捷键展示 -->
                  <td class="kbd-keys">
                    <span
                      v-for="(k, ki) in renderKeys(item)"
                      :key="ki"
                      class="kbd-key-group"
                    >
                      <kbd>{{ k }}</kbd>
                      <!-- 多键之间添加 "+" 分隔符（最后一个 key 不加）-->
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

          <!-- 底部提示 -->
          <div class="kbd-footer">
            <span>按 <kbd>?</kbd> 或通过命令面板（<kbd>Ctrl</kbd>+<kbd>K</kbd>）快速打开此面板</span>
          </div>
        </div>
      </NScrollbar>
    </NModal>
  </Teleport>
</template>

<style scoped>
/* ─── 弹窗主体 ─── */
.kbd-help-body {
  padding: 4px 0 8px;
}

/* ─── 分组容器 ─── */
.kbd-group {
  margin-bottom: 24px;
}
.kbd-group:last-of-type {
  margin-bottom: 0;
}

/* ─── 分组标题 ─── */
.kbd-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  opacity: 0.55;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(128, 128, 128, 0.12);
}

/* ─── 快捷键表格 ─── */
.kbd-table {
  width: 100%;
  border-collapse: collapse;
}
.kbd-row {
  border-bottom: 1px solid rgba(128, 128, 128, 0.06);
}
.kbd-row:last-child {
  border-bottom: none;
}

/* ─── 说明列 ─── */
.kbd-desc {
  padding: 8px 12px 8px 4px;
  font-size: 14px;
  line-height: 1.5;
  width: 100%;
}

/* ─── 快捷键列 ─── */
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
  opacity: 0.4;
  margin: 0 1px;
}

/* ─── kbd 标签样式 ─── */
kbd {
  display: inline-block;
  padding: 2px 7px;
  border: 1px solid rgba(128, 128, 128, 0.3);
  border-bottom-width: 2px;
  border-radius: 5px;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 12px;
  background: rgba(128, 128, 128, 0.07);
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

/* ─── 底部提示 ─── */
.kbd-footer {
  margin-top: 20px;
  padding: 10px 12px;
  background: rgba(128, 128, 128, 0.05);
  border-radius: 6px;
  font-size: 12px;
  opacity: 0.6;
  text-align: center;
  line-height: 1.6;
}

/* ─── 移动端适配 ─── */
@media (max-width: 767px) {
  .kbd-desc { font-size: 13px; }
  .kbd-keys { font-size: 11px; }
}
</style>
