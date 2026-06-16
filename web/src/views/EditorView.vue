<script setup lang="ts">
/**
 * 章节编辑器页面（P8-06/07/08/09/14）。
 * - P8-06：TipTap 编辑器，只加载当前章节，auto-save debounce 1s。
 * - P8-07：左侧图标面板（大纲/角色库/词典侧栏）。
 * - P8-08：AI 浮窗 + SSE 续写 + RAF 批量写入。
 * - P8-09：全书搜索替换横条（300ms debounce，Esc 关闭）。
 * - P8-14：移动端响应式，侧栏改为底部抽屉，编辑区全宽显示。
 */
import {computed, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import type {Editor} from '@tiptap/vue-3'
import {NButton, NDrawer, NDrawerContent, NEmpty, NInput, NLayout, NLayoutContent, NSpace, NSpin, useMessage} from 'naive-ui'
import ChapterEditor from '@/components/ChapterEditor.vue'
import EditorSidePanel from '@/components/EditorSidePanel.vue'
import AiFloatButton from '@/components/AiFloatButton.vue'
import SearchReplaceBar from '@/components/SearchReplaceBar.vue'
import SnapshotDrawer from '@/components/SnapshotDrawer.vue'
import type {ChapterInfo, OutlineInfo} from '@/api/project'
import {getChapter, getOutline, saveChapter} from '@/api/project'
import {useDevice} from '@/composables/useDevice'
import {useCommandPaletteStore} from '@/stores/commandPalette'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const { isMobile } = useDevice()
const palette = useCommandPaletteStore()

const chapter = ref<ChapterInfo | null>(null)
const outline = ref<OutlineInfo | null>(null)
const loading = ref(false)
const chapterTitle = ref('')
const editorRef = ref<InstanceType<typeof ChapterEditor> | null>(null)

// P8-08: AI 浮窗状态
const aiVisible = ref(false)
// P8-11: 版本快照抽屉
const showSnapshot = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const editorInstance = ref<any>(undefined)

// P8-09: 搜索替换横条
const showSearch = ref(false)
const showReplace = ref(false)

// P8-14: 移动端侧栏抽屉状态
const showMobileSidePanel = ref(false)

const projectId = route.params.projectId as string
const currentChapterId = computed(() => route.params.chapterId as string | undefined)

async function loadChapter(id: string) {
  loading.value = true
  try {
    const res = await getChapter(id)
    chapter.value = res.data.data
    chapterTitle.value = chapter.value.title ?? ''
  } catch {
    message.error('章节加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOutline() {
  try {
    const res = await getOutline(projectId)
    outline.value = res.data.data
  } catch { /* 静默失败 */ }
}

async function handleAutoSave(content: string) {
  if (!chapter.value) return
  try {
    await saveChapter(chapter.value.id, {
      title: chapterTitle.value || undefined,
      content,
      status: chapter.value.status,
    })
    editorRef.value?.markSaved()
  } catch {
    editorRef.value?.markError()
  }
}

async function handleTitleBlur() {
  if (!chapter.value) return
  try {
    await saveChapter(chapter.value.id, { title: chapterTitle.value })
    chapter.value.title = chapterTitle.value
  } catch {
    message.error('标题保存失败')
  }
}

/** P8-07 侧栏章节选中。 */
function handleSelectChapter(chapterId: string) {
  router.push(`/projects/${projectId}/editor/${chapterId}`)
}

/** P8-08 检测文本选中显示 AI 按钮。 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function handleEditorMounted(editor: any) {
  editorInstance.value = editor
  editor.on('selectionUpdate', ({ editor: e }: { editor: Editor }) => {
    const { from, to } = e.state.selection
    aiVisible.value = from !== to
  })
}

/** P8-09 键盘快捷键：Ctrl/Cmd+F 打开搜索。 */
function handleGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
    e.preventDefault()
    showSearch.value = true
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'h') {
    e.preventDefault()
    showSearch.value = true
    showReplace.value = true
  }
}

onMounted(async () => {
  document.addEventListener('keydown', handleGlobalKeydown)
  await loadOutline()
  if (currentChapterId.value) await loadChapter(currentChapterId.value)
  // P8-15：注册编辑器上下文命令
  palette.registerCommands([
    {
      id: 'editor:search',
      label: '搜索替换',
      description: '在全书中搜索/替换文字',
      group: '写作',
      icon: '🔍',
      shortcut: 'Ctrl+F',
      action: () => { showSearch.value = true },
    },
    {
      id: 'editor:snapshot',
      label: '查看版本历史',
      description: '查看章节快照并支持恢复',
      group: '写作',
      icon: '🕐',
      shortcut: '',
      action: () => { showSnapshot.value = true },
    },
    {
      id: 'editor:side-panel',
      label: '切换侧栏',
      description: '显示/隐藏大纲和角色库',
      group: '写作',
      icon: '📋',
      shortcut: '',
      action: () => { showMobileSidePanel.value = !showMobileSidePanel.value },
    },
  ])
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
  palette.unregisterCommands(['editor:search', 'editor:snapshot', 'editor:side-panel'])
})

watch(currentChapterId, async (newId) => {
  if (newId) await loadChapter(newId)
})
</script>

<template>
  <NLayout style="height: 100vh; display: flex; flex-direction: row; overflow: hidden">

    <!-- P8-07 左侧图标面板 + 侧栏（仅桌面端，P8-14 在移动端隐藏）-->
    <EditorSidePanel
      v-if="!isMobile"
      :project-id="projectId"
      :chapter-id="currentChapterId"
      :outline="outline"
      @select-chapter="handleSelectChapter"
    />

    <!-- 编辑器主区域 -->
    <NLayoutContent style="flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0">

      <!-- P8-09 搜索替换横条 -->
      <SearchReplaceBar
        v-if="showSearch"
        :project-id="projectId"
        :show-replace="showReplace"
        @close="showSearch = false; showReplace = false"
        @jump-to-chapter="handleSelectChapter"
      />

      <!-- 章节标题 + 工具栏（P8-11 快照入口 / P8-14 移动端侧栏按钮）-->
      <div class="editor-toolbar">
        <!-- P8-14 移动端：返回按钮 + 侧栏切换按钮 -->
        <NButton v-if="isMobile" size="small" text @click="router.back()">← 返回</NButton>
        <NInput
          v-model:value="chapterTitle"
          placeholder="章节标题"
          :bordered="false"
          :style="{ fontSize: isMobile ? '16px' : '20px', fontWeight: '600', flex: '1' }"
          @blur="handleTitleBlur"
        />
        <NSpace v-if="currentChapterId" :size="8" align="center">
          <!-- P8-14 移动端：大纲按钮 -->
          <NButton v-if="isMobile" size="small" text
                   title="打开侧栏"
                   @click="showMobileSidePanel = true">📋</NButton>
          <NButton size="small" text title="版本快照与 diff（P8-11）"
                   @click="showSnapshot = true">🕐 历史</NButton>
        </NSpace>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" style="flex: 1; display: flex; align-items: center; justify-content: center">
        <NSpin size="large" />
      </div>

      <!-- 未选择章节 -->
      <div v-else-if="!currentChapterId" style="flex: 1; display: flex; align-items: center; justify-content: center">
        <NEmpty :description="isMobile ? '请点击 📋 选择章节' : '请从左侧选择一个章节开始写作'">
          <template #extra>
            <span style="font-size: 12px; color: #999">
              {{ isMobile ? '点击上方 📋 展开大纲' : 'Ctrl+F 全书搜索 · Ctrl+H 搜索替换' }}
            </span>
          </template>
        </NEmpty>
      </div>

      <!-- TipTap 编辑器（P8-06）-->
      <template v-else>
        <div style="flex: 1; overflow: hidden; position: relative">
          <ChapterEditor
            ref="editorRef"
            :chapter="chapter"
            style="height: 100%"
            @change="handleAutoSave"
            @editor-ready="handleEditorMounted"
          />

          <!-- P8-08 AI 浮窗 -->
          <AiFloatButton
            :editor="editorInstance"
            :chapter-id="currentChapterId"
            :visible="aiVisible"
            @stream-start="aiVisible = false"
            @stream-end="aiVisible = true"
          />
        </div>
      </template>

    </NLayoutContent>
  </NLayout>

  <!-- P8-11 版本快照抽屉（放在 NLayout 外以避免层叠问题）-->
  <SnapshotDrawer
    v-if="currentChapterId"
    v-model:show="showSnapshot"
    :chapter-id="currentChapterId"
    :current-content="chapter?.content ?? ''"
    @restored="() => { if (currentChapterId) loadChapter(currentChapterId) }"
  />

  <!-- P8-14 移动端侧栏抽屉（从右侧滑入）-->
  <NDrawer v-if="isMobile" v-model:show="showMobileSidePanel" :width="300" placement="right">
    <NDrawerContent title="大纲与角色" :native-scrollbar="false">
      <EditorSidePanel
        :project-id="projectId"
        :chapter-id="currentChapterId"
        :outline="outline"
        @select-chapter="(id) => { handleSelectChapter(id); showMobileSidePanel = false }"
      />
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
/* ─── 编辑器工具栏（P8-14 响应式）─── */
.editor-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (min-width: 768px) {
  .editor-toolbar {
    padding: 8px 24px;
    gap: 12px;
  }
}
</style>
