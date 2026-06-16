<script setup lang="ts">
/**
 * 版本快照抽屉组件（P8-11）。
 * - 显示章节历史快照列表
 * - 支持快照内容预览（diff 高亮 Only Changes 模式）
 * - 支持手动创建快照
 * - 支持恢复快照（恢复前后端自动保存当前状态为快照）
 *
 * diff 实现：纯前端文字级逐行比对，高亮新增/删除行，无需引入外部 diff 库。
 */
import { computed, ref, watch } from 'vue'
import {
  NButton, NDrawer, NDrawerContent, NEmpty, NInput, NModal, NScrollbar,
  NSpace, NSpin, NTag, NText, NIcon, useDialog, useMessage,
} from 'naive-ui'
import {
  Plus,
  Eye,
  RotateCcw,
} from 'lucide-vue-next'
import type { SnapshotInfo } from '@/api/snapshot'
import { createSnapshot, listSnapshots, restoreSnapshot } from '@/api/snapshot'

const props = defineProps<{
  /** 控制抽屉是否显示。 */
  show: boolean
  /** 当前章节 ID。 */
  chapterId: string
  /** 当前章节正文（供 diff 对比）。 */
  currentContent: string
}>()

const emit = defineEmits<{
  /** 关闭抽屉。 */
  'update:show': [value: boolean]
  /** 快照恢复完成，通知父组件刷新章节。 */
  restored: []
}>()

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const snapshots = ref<SnapshotInfo[]>([])
const previewSnapshot = ref<SnapshotInfo | null>(null)
const showPreview = ref(false)
const showCreate = ref(false)
const createLabel = ref('')
const creating = ref(false)
const restoring = ref(false)
const diffOnly = ref(true)

watch(() => props.show, async (val) => {
  if (val && props.chapterId) await loadSnapshots()
})

async function loadSnapshots() {
  loading.value = true
  try {
    const res = await listSnapshots(props.chapterId)
    snapshots.value = res.data.data
  } catch {
    message.error('快照列表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCreateSnapshot() {
  creating.value = true
  try {
    await createSnapshot(props.chapterId, createLabel.value || undefined)
    message.success('快照已创建')
    showCreate.value = false
    createLabel.value = ''
    await loadSnapshots()
  } catch {
    message.error('快照创建失败')
  } finally {
    creating.value = false
  }
}

function handleRestoreSnapshot(snapshot: SnapshotInfo) {
  dialog.warning({
    title: '确认恢复',
    content: `恢复到「${snapshot.label || formatTime(snapshot.createdAt)}」版本？恢复前将自动保存当前版本为新快照。`,
    positiveText: '恢复',
    negativeText: '取消',
    onPositiveClick: async () => {
      restoring.value = true
      try {
        await restoreSnapshot(snapshot.id)
        message.success('快照已恢复')
        emit('restored')
        emit('update:show', false)
      } catch {
        message.error('快照恢复失败')
      } finally {
        restoring.value = false
      }
    },
  })
}

function openPreview(snapshot: SnapshotInfo) {
  previewSnapshot.value = snapshot
  showPreview.value = true
}

const diffLines = computed<Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }>>(() => {
  if (!previewSnapshot.value) return []
  const oldLines = (previewSnapshot.value.content ?? '').split('\n')
  const newLines = stripHtmlTags(props.currentContent).split('\n')
  return computeDiff(oldLines, newLines)
})

const visibleDiffLines = computed(() => {
  if (!diffOnly.value) return diffLines.value
  const lines = diffLines.value
  const include = new Set<number>()
  lines.forEach((l, i) => {
    if (l.type !== 'unchanged') {
      for (let j = Math.max(0, i - 2); j <= Math.min(lines.length - 1, i + 2); j++) {
        include.add(j)
      }
    }
  })
  return lines.filter((_, i) => include.has(i))
})

function computeDiff(
  oldLines: string[],
  newLines: string[],
): Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }> {
  const result: Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }> = []
  let i = 0; let j = 0
  while (i < oldLines.length || j < newLines.length) {
    if (i >= oldLines.length) {
      result.push({ text: newLines[j++], type: 'added' })
    } else if (j >= newLines.length) {
      result.push({ text: oldLines[i++], type: 'removed' })
    } else if (oldLines[i] === newLines[j]) {
      result.push({ text: oldLines[i++], type: 'unchanged' })
      j++
    } else {
      const lookAhead = 3
      let matched = false
      for (let d = 1; d <= lookAhead && !matched; d++) {
        if (i + d < oldLines.length && oldLines[i + d] === newLines[j]) {
          for (let k = 0; k < d; k++) result.push({ text: oldLines[i++], type: 'removed' })
          matched = true
        } else if (j + d < newLines.length && newLines[j + d] === oldLines[i]) {
          for (let k = 0; k < d; k++) result.push({ text: newLines[j++], type: 'added' })
          matched = true
        }
      }
      if (!matched) {
        result.push({ text: oldLines[i++], type: 'removed' })
        result.push({ text: newLines[j++], type: 'added' })
      }
    }
  }
  return result
}

function stripHtmlTags(html: string): string {
  return html
    .replace(/<\/p>/gi, '\n')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&nbsp;/g, ' ')
    .trim()
}

function formatTime(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ` +
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function snapshotTypeLabel(type: string): string {
  const map: Record<string, string> = {
    manual: '手动存档',
    auto_before_ai: 'AI 改编前',
    polish_accepted: '润色前',
    auto_before_replace: '替换前',
    auto_before_restore: '恢复前',
  }
  return map[type] ?? type
}
</script>

<template>
  <!-- 快照列表抽屉 -->
  <NDrawer
    :show="show"
    width="340"
    placement="right"
    @update:show="emit('update:show', $event)"
  >
    <NDrawerContent title="版本快照" :native-scrollbar="false">
      <template #footer>
        <NButton type="primary" size="small" @click="showCreate = true">
          <template #icon>
            <NIcon :component="Plus" :size="14" />
          </template>
          手动创建快照
        </NButton>
      </template>

      <NSpin :show="loading">
        <NEmpty v-if="!loading && !snapshots.length" description="暂无快照记录" />

        <div v-for="snap in snapshots" :key="snap.id" class="snapshot-item">
          <div class="snapshot-meta">
            <NText strong style="font-size: 13px">
              {{ snap.label || formatTime(snap.createdAt) }}
            </NText>
            <NTag v-if="snap.label?.startsWith('auto')" size="tiny" type="default">自动</NTag>
            <NTag v-if="snap.snapshotType" size="tiny" type="info">
              {{ snapshotTypeLabel(snap.snapshotType) }}
            </NTag>
          </div>
          <NText depth="3" style="font-size: 12px">
            {{ snap.wordCount }} 字 · {{ formatTime(snap.createdAt) }}
          </NText>
          <NSpace :size="6" style="margin-top: 6px">
            <NButton size="tiny" @click="openPreview(snap)">
              <template #icon>
                <NIcon :component="Eye" :size="12" />
              </template>
              对比预览
            </NButton>
            <NButton size="tiny" type="warning" :loading="restoring" @click="handleRestoreSnapshot(snap)">
              <template #icon>
                <NIcon :component="RotateCcw" :size="12" />
              </template>
              恢复此版本
            </NButton>
          </NSpace>
        </div>
      </NSpin>
    </NDrawerContent>
  </NDrawer>

  <!-- 创建快照弹窗 -->
  <NModal
    v-model:show="showCreate"
    preset="dialog"
    title="创建快照"
    positive-text="创建"
    negative-text="取消"
    :loading="creating"
    @positive-click="handleCreateSnapshot"
  >
    <NInput
      v-model:value="createLabel"
      placeholder="快照备注（可选，如：大改前）"
      clearable
      style="margin-top: 8px"
    />
  </NModal>

  <!-- diff 预览弹窗 -->
  <NModal
    v-model:show="showPreview"
    :title="`对比：${previewSnapshot?.label || formatTime(previewSnapshot?.createdAt ?? '')} ↔ 当前版本`"
    style="width: 720px; max-width: 96vw"
    preset="card"
    :bordered="false"
  >
    <template #header-extra>
      <NButton
        size="small"
        :type="diffOnly ? 'primary' : 'default'"
        @click="diffOnly = !diffOnly"
      >
        {{ diffOnly ? '显示全部' : '只看差异' }}
      </NButton>
    </template>

    <NScrollbar style="max-height: 60vh">
      <div v-if="visibleDiffLines.length" class="diff-list">
        <div
          v-for="(line, idx) in visibleDiffLines"
          :key="idx"
          :class="['diff-line', `diff-line--${line.type}`]"
        >
          <span class="diff-marker">{{ line.type === 'added' ? '+' : line.type === 'removed' ? '-' : ' ' }}</span>
          {{ line.text }}
        </div>
      </div>
      <NEmpty v-else description="两个版本内容完全相同" />
    </NScrollbar>
  </NModal>
</template>

<style scoped>
.snapshot-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.snapshot-item:last-child {
  border-bottom: none;
}

.snapshot-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.diff-list {
  font-family: var(--w-font-mono);
  font-size: 13px;
  line-height: 1.7;
}

.diff-line {
  padding: 1px 8px;
  white-space: pre-wrap;
  word-break: break-all;
  border-left: 3px solid transparent;
}

.diff-line--added {
  background: var(--w-success-soft);
  border-left-color: var(--w-success);
  color: var(--w-success);
}

.diff-line--removed {
  background: var(--w-danger-soft);
  border-left-color: var(--w-danger);
  color: var(--w-danger);
}

.diff-line--unchanged {
  color: var(--w-text-secondary);
}

.diff-marker {
  display: inline-block;
  width: 14px;
  margin-right: 8px;
  font-weight: 600;
}
</style>
