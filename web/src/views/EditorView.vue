<script setup lang="ts">
/**
 * 章节编辑器页面。
 *
 * 特性：
 * - TipTap 编辑器，只加载当前章节，auto-save debounce 1s
 * - 左侧图标面板（大纲/角色库/词典侧栏）
 * - 创作辅助浮窗 + SSE 续写
 * - 全书搜索替换横条
 * - 移动端响应式
 * - 渐进式用户引导
 */
import { computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Editor } from '@tiptap/vue-3'
import { NButton, NDrawer, NDrawerContent, NEmpty, NInput, NIcon, NSpin, useMessage } from 'naive-ui'
import {
  ArrowLeft,
  PanelLeft,
  History,
  AlertTriangle,
  Flame,
  FileUp,
} from 'lucide-vue-next'
import ChapterEditor from '@/components/ChapterEditor.vue'
import EditorSidePanel from '@/components/EditorSidePanel.vue'
import AiFloatButton from '@/components/AiFloatButton.vue'
import SearchReplaceBar from '@/components/SearchReplaceBar.vue'
import OnboardingHint from '@/components/OnboardingHint.vue'
import ImportContentDrawer from '@/components/ImportContentDrawer.vue'
import type { ChapterInfo, OutlineInfo } from '@/api/project'
import { getChapter, getOutline, saveChapter } from '@/api/project'
import { getWritingOverview } from '@/api/stats'
import { useDevice } from '@/composables/useDevice'
import { useCommandPaletteStore } from '@/stores/commandPalette'
import { useOnboarding } from '@/composables/useOnboarding'
import { useEditorHeartbeat } from '@/composables/useEditorHeartbeat'

const SnapshotDrawer = defineAsyncComponent(
  () => import('@/components/SnapshotDrawer.vue'),
)

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

const aiVisible = ref(false)
const showSnapshot = ref(false)
const snapshotEverOpened = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const editorInstance = ref<any>(undefined)

const showSearch = ref(false)
const showReplace = ref(false)
const showImport = ref(false)

const showMobileSidePanel = ref(false)

const streakReminderVisible = ref(false)
const streakDays = ref(0)
const todayRemainingChars = ref(0)

const ob = useOnboarding()
const showFirstEditorHint = ref(false)
const showFirstAiSelectHint = ref(false)
const showMilestone3000 = ref(false)
const maxTrackedChars = ref(0)

const projectId = route.params.projectId as string
const currentChapterId = computed(() => route.params.chapterId as string | undefined)

const { otherTabEditing } = useEditorHeartbeat(currentChapterId.value ?? '')

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

  const charCount = content.replace(/\s/g, '').length
  if (charCount > maxTrackedChars.value) {
    maxTrackedChars.value = charCount
    if (charCount >= 3000 && ob.shouldShow('char-milestone-3000')) {
      showMilestone3000.value = true
    }
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

function handleSelectChapter(chapterId: string) {
  router.push(`/projects/${projectId}/editor/${chapterId}`)
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function handleEditorMounted(editor: any) {
  editorInstance.value = editor
  editor.on('selectionUpdate', ({ editor: e }: { editor: Editor }) => {
    const { from, to } = e.state.selection
    const hasSelection = from !== to
    aiVisible.value = hasSelection
    if (hasSelection && ob.shouldShow('first-ai-select')) {
      showFirstAiSelectHint.value = true
    }
  })
}

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
  if (route.query.import === '1') showImport.value = true

  try {
    const statsRes = await getWritingOverview()
    const overview = statsRes.data.data
    if (overview.streak > 0) {
      const todayProgress = overview.todayChars ?? 0
      const goal = overview.dailyGoal ?? 2000
      if (todayProgress < goal) {
        streakDays.value = overview.streak
        todayRemainingChars.value = goal - todayProgress
        streakReminderVisible.value = true
      }
    }
  } catch { /* 静默失败 */ }

  if (ob.shouldShow('first-editor')) {
    showFirstEditorHint.value = true
  }

  palette.registerCommands([
    {
      id: 'editor:import',
      label: '导入稿件',
      description: '上传或粘贴已有正文，切分为章节',
      group: '写作',
      icon: 'I',
      shortcut: '',
      action: () => { showImport.value = true },
    },
    {
      id: 'editor:search',
      label: '搜索替换',
      description: '在全书中搜索/替换文字',
      group: '写作',
      icon: 'S',
      shortcut: 'Ctrl+F',
      action: () => { showSearch.value = true },
    },
    {
      id: 'editor:snapshot',
      label: '查看版本历史',
      description: '查看章节快照并支持恢复',
      group: '写作',
      icon: 'H',
      shortcut: '',
      action: () => { snapshotEverOpened.value = true; showSnapshot.value = true },
    },
    {
      id: 'editor:side-panel',
      label: '切换侧栏',
      description: '显示/隐藏大纲和角色库',
      group: '写作',
      icon: 'P',
      shortcut: '',
      action: () => { showMobileSidePanel.value = !showMobileSidePanel.value },
    },
  ])
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
  palette.unregisterCommands(['editor:import', 'editor:search', 'editor:snapshot', 'editor:side-panel'])
})

watch(currentChapterId, async (newId) => {
  if (newId) await loadChapter(newId)
})
</script>

<template>
  <div class="editor-page">
    <!-- 左侧图标面板 + 侧栏 -->
    <EditorSidePanel
      v-if="!isMobile"
      :project-id="projectId"
      :chapter-id="currentChapterId"
      :outline="outline"
      @select-chapter="handleSelectChapter"
      @open-search="showSearch = true"
      @open-import="showImport = true"
    />

    <!-- 编辑器主区域 -->
    <div class="editor-main">
      <!-- 多标签页冲突警告 -->
      <div v-if="otherTabEditing" class="other-tab-warning">
        <AlertTriangle :size="14" />
        <span>另一个标签页正在编辑本章节，请注意避免内容冲突</span>
      </div>

      <!-- 连续写作提醒条 -->
      <div v-if="streakReminderVisible" class="streak-reminder">
        <Flame :size="14" />
        <span>你已连续更新 {{ streakDays }} 天，今天还差 {{ todayRemainingChars }} 字</span>
        <button class="streak-reminder-close" @click="streakReminderVisible = false">×</button>
      </div>

      <!-- 搜索替换横条 -->
      <SearchReplaceBar
        v-if="showSearch"
        :project-id="projectId"
        :show-replace="showReplace"
        @close="showSearch = false; showReplace = false"
        @jump-to-chapter="handleSelectChapter"
      />

      <!-- 章节标题 + 工具栏 -->
      <div class="editor-toolbar">
        <NButton v-if="isMobile" text class="toolbar-btn" @click="router.back()">
          <template #icon>
            <NIcon :component="ArrowLeft" :size="18" />
          </template>
        </NButton>

        <NInput
          v-model:value="chapterTitle"
          placeholder="章节标题"
          :bordered="false"
          class="chapter-title-input"
          :style="{ fontSize: isMobile ? '16px' : '20px' }"
          @blur="handleTitleBlur"
        />

        <div v-if="currentChapterId" class="toolbar-actions">
          <NButton v-if="isMobile" text class="toolbar-btn" title="打开侧栏" @click="showMobileSidePanel = true">
            <template #icon>
              <NIcon :component="PanelLeft" :size="18" />
            </template>
          </NButton>
          <NButton text class="toolbar-btn" title="导入稿件" @click="showImport = true">
            <template #icon>
              <NIcon :component="FileUp" :size="18" />
            </template>
            <span v-if="!isMobile">导入</span>
          </NButton>
          <NButton text class="toolbar-btn" title="版本快照与 diff" @click="snapshotEverOpened = true; showSnapshot = true">
            <template #icon>
              <NIcon :component="History" :size="18" />
            </template>
            <span v-if="!isMobile">历史</span>
          </NButton>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="editor-loading">
        <NSpin size="large" />
      </div>

      <!-- 未选择章节 -->
      <div v-else-if="!currentChapterId" class="editor-empty">
        <NEmpty :description="isMobile ? '请点击侧栏选择章节' : '请从左侧选择一个章节开始写作'">
          <template #extra>
            <span class="editor-empty-tip">
              {{ isMobile ? '点击上方侧栏按钮展开大纲' : 'Ctrl+F 全书搜索 · Ctrl+H 搜索替换' }}
            </span>
          </template>
        </NEmpty>
      </div>

      <!-- TipTap 编辑器 -->
      <template v-else>
        <div v-if="showFirstEditorHint || showFirstAiSelectHint || showMilestone3000" class="onboarding-area">
          <OnboardingHint
            v-if="showFirstEditorHint"
            icon="info"
            title="开始写作"
            description="在编辑器中尽情写作。使用 Ctrl+F 搜索，Ctrl+H 替换，历史按钮查看版本快照。"
            action-label="我知道了"
            variant="info"
            @close="showFirstEditorHint = false; ob.markDone('first-editor')"
          />
          <OnboardingHint
            v-if="showFirstAiSelectHint"
            icon="lightbulb"
            title="创作辅助"
            description="选中文字后，会出现轻量操作条。可以续写、润色或提出改写要求，生成内容会保留标识，确认后再融入正文。"
            action-label="明白了"
            variant="info"
            @close="showFirstAiSelectHint = false; ob.markDone('first-ai-select')"
          />
          <OnboardingHint
            v-if="showMilestone3000"
            icon="check"
            title="已写超过 3000 字"
            description="太棒了！你已经写超过 3000 字，坚持每天的写作目标，你的故事正在成形。"
            variant="success"
            @close="showMilestone3000 = false; ob.markDone('char-milestone-3000')"
          />
        </div>

        <div class="editor-wrapper">
          <ChapterEditor
            ref="editorRef"
            :chapter="chapter"
            @change="handleAutoSave"
            @editor-ready="handleEditorMounted"
          />

          <AiFloatButton
            :editor="editorInstance"
            :chapter-id="currentChapterId"
            :visible="aiVisible"
            @stream-start="aiVisible = false"
            @stream-end="aiVisible = true"
          />
        </div>
      </template>
    </div>

    <!-- 版本快照抽屉 -->
    <SnapshotDrawer
      v-if="snapshotEverOpened && currentChapterId"
      v-model:show="showSnapshot"
      :chapter-id="currentChapterId"
      :current-content="chapter?.content ?? ''"
      @restored="() => { if (currentChapterId) loadChapter(currentChapterId) }"
    />

    <!-- 移动端侧栏抽屉 -->
    <NDrawer v-if="isMobile" v-model:show="showMobileSidePanel" :width="300" placement="right">
      <NDrawerContent title="大纲与角色" :native-scrollbar="false">
        <EditorSidePanel
          :project-id="projectId"
          :chapter-id="currentChapterId"
          :outline="outline"
          @select-chapter="(id) => { handleSelectChapter(id); showMobileSidePanel = false }"
          @open-search="() => { showSearch = true; showMobileSidePanel = false }"
          @open-import="() => { showImport = true; showMobileSidePanel = false }"
        />
      </NDrawerContent>
    </NDrawer>

    <ImportContentDrawer
      v-model:show="showImport"
      :project-id="projectId"
      :outline="outline"
      @imported="loadOutline"
    />
  </div>
</template>

<style scoped>
.editor-page {
  display: flex;
  height: calc(100vh - var(--w-topbar-height));
  overflow: hidden;
  background: var(--w-bg-canvas);
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 多标签页冲突警告 */
.other-tab-warning {
  background: var(--w-warning-soft);
  color: var(--w-warning);
  font-size: var(--w-text-sm);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--w-border-subtle);
  flex-shrink: 0;
}

/* 连续写作提醒条 */
.streak-reminder {
  background: var(--w-bg-secondary);
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--w-border-subtle);
  flex-shrink: 0;
}

.streak-reminder-close {
  margin-left: auto;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--w-radius-sm);
  color: var(--w-text-tertiary);
  font-size: 16px;
  transition: all var(--w-transition-fast);
}

.streak-reminder-close:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

/* 编辑器工具栏 */
.editor-toolbar {
  height: 58px;
  padding: 0 var(--w-space-4);
  border-bottom: 1px solid var(--w-border-subtle);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: var(--w-space-2);
  background: var(--w-bg-toolbar);
}

.chapter-title-input {
  flex: 1;
  font-weight: 600;
  background: transparent !important;
}

.chapter-title-input :deep(.n-input__input-el) {
  color: var(--w-text) !important;
  font-weight: 600;
  font-family: var(--w-font-serif);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--w-space-1);
}

.toolbar-btn {
  color: var(--w-text-secondary) !important;
}

.toolbar-btn:hover {
  color: var(--w-text) !important;
  background: var(--w-bg-hover) !important;
}

/* 加载与空状态 */
.editor-loading,
.editor-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.editor-empty-tip {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

/* 引导提示区 */
.onboarding-area {
  padding: var(--w-space-3) var(--w-space-4);
  flex-shrink: 0;
  background: var(--w-bg-canvas);
}

.onboarding-area .ob-hint {
  margin: 0;
}

/* 编辑器包装 */
.editor-wrapper {
  flex: 1;
  overflow: hidden;
  position: relative;
}

@media (max-width: 767px) {
  .editor-page {
    height: calc(100vh - var(--w-topbar-height) - 56px);
  }

  .editor-toolbar {
    padding: 0 var(--w-space-3);
  }

  .onboarding-area {
    padding: var(--w-space-2) var(--w-space-3);
  }
}
</style>
