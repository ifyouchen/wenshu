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
import {computed, ref, watch} from 'vue'
import {NButton, NDrawer, NDrawerContent, NEmpty, NInput, NModal, NScrollbar, NSpace, NSpin, NTag, NText, useDialog, useMessage,} from 'naive-ui'
import type {SnapshotInfo} from '@/api/snapshot'
import {createSnapshot, listSnapshots, restoreSnapshot} from '@/api/snapshot'

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
/** 当前预览的快照。 */
const previewSnapshot = ref<SnapshotInfo | null>(null)
const showPreview = ref(false)
const showCreate = ref(false)
const createLabel = ref('')
const creating = ref(false)
const restoring = ref(false)
/** 是否只显示差异行。 */
const diffOnly = ref(true)

/** 监听抽屉打开，自动加载快照列表。 */
watch(() => props.show, async (val) => {
  if (val && props.chapterId) await loadSnapshots()
})

/** 加载快照列表。 */
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

/** 手动创建快照。 */
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

/** 恢复快照（弹确认对话框）。 */
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

/** 打开快照预览（diff 模式）。 */
function openPreview(snapshot: SnapshotInfo) {
  previewSnapshot.value = snapshot
  showPreview.value = true
}

/**
 * 计算 diff 结果。
 * 基于按行分割的 LCS 简化算法：仅标记增删行（不支持移动）。
 * - type 'added'：仅出现在新版本（当前内容）的行。
 * - type 'removed'：仅出现在旧版本（快照内容）的行。
 * - type 'unchanged'：两者相同的行。
 */
const diffLines = computed<Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }>>(() => {
  if (!previewSnapshot.value) return []
  const oldLines = (previewSnapshot.value.content ?? '').split('\n')
  const newLines = stripHtmlTags(props.currentContent).split('\n')
  return computeDiff(oldLines, newLines)
})

/** 过滤后仅包含变更行（diffOnly 模式）。 */
const visibleDiffLines = computed(() => {
  if (!diffOnly.value) return diffLines.value
  // 保留变更行及其前后 2 行上下文
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

/** 简化 LCS diff（逐行比对）。 */
function computeDiff(
  oldLines: string[],
  newLines: string[],
): Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }> {
  const result: Array<{ text: string; type: 'added' | 'removed' | 'unchanged' }> = []
  // 使用双指针贪心近似（非完整 LCS，但对网文连续文本足够）
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
      // 向前看 3 行尝试对齐
      const lookAhead = 3
      let matched = false
      for (let d = 1; d <= lookAhead && !matched; d++) {
        if (i + d < oldLines.length && oldLines[i + d] === newLines[j]) {
          // 旧版本有 d 行删除
          for (let k = 0; k < d; k++) result.push({ text: oldLines[i++], type: 'removed' })
          matched = true
        } else if (j + d < newLines.length && newLines[j + d] === oldLines[i]) {
          // 新版本有 d 行新增
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

/** 去除 HTML 标签，还原纯文本。 */
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

/** 格式化时间为可读字符串。 */
function formatTime(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ` +
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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
          + 手动创建快照
        </NButton>
      </template>

      <NSpin :show="loading">
        <NEmpty v-if="!loading && !snapshots.length" description="暂无快照记录" />

        <div v-for="snap in snapshots" :key="snap.id" class="snapshot-item">
          <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap">
            <NText strong style="font-size: 13px">
              {{ snap.label || formatTime(snap.createdAt) }}
            </NText>
            <NTag v-if="snap.label?.startsWith('auto')" size="tiny" type="default">自动</NTag>
          </div>
          <NText depth="3" style="font-size: 12px">
            {{ snap.wordCount }} 字 · {{ formatTime(snap.createdAt) }}
          </NText>
          <NSpace :size="6" style="margin-top: 6px">
            <NButton size="tiny" @click="openPreview(snap)">对比预览</NButton>
            <NButton size="tiny" type="warning" :loading="restoring" @click="handleRestoreSnapshot(snap)">
              恢复此版本
            </NButton>
          </NSpace>
        </div>
      </NSpin>
    </NDrawerContent>
  </NDrawer>

  <!-- 创建快照弹窗 -->
  <NModal v-model:show="showCreate" preset="dialog" title="创建快照" positive-text="创建" negative-text="取消"
          :loading="creating" @positive-click="handleCreateSnapshot">
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
      <div v-if="visibleDiffLines.length" style="font-family: monospace; font-size: 13px; line-height: 1.7">
        <div
          v-for="(line, idx) in visibleDiffLines"
          :key="idx"
          :style="{
            background: line.type === 'added' ? 'rgba(24,160,88,0.12)' : line.type === 'removed' ? 'rgba(245,63,63,0.10)' : 'transparent',
            padding: '1px 8px',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
            borderLeft: line.type === 'added' ? '3px solid #18a058' : line.type === 'removed' ? '3px solid #f53f3f' : '3px solid transparent',
          }"
        >
          <span :style="{ color: line.type === 'added' ? '#18a058' : line.type === 'removed' ? '#f53f3f' : '#666', marginRight: '8px' }">
            {{ line.type === 'added' ? '+' : line.type === 'removed' ? '-' : ' ' }}
          </span>{{ line.text }}
        </div>
      </div>
      <NEmpty v-else description="两个版本内容完全相同" />
    </NScrollbar>
  </NModal>
</template>

<style scoped>
.snapshot-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.snapshot-item:last-child { border-bottom: none; }
</style>

