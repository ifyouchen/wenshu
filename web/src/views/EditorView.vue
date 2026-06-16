<script setup lang="ts">
/**
 * 章节编辑器页面（P8-06/07/08/09/14/18/22）。
 * - P8-06：TipTap 编辑器，只加载当前章节，auto-save debounce 1s。
 * - P8-07：左侧图标面板（大纲/角色库/词典侧栏）。
 * - P8-08：AI 浮窗 + SSE 续写 + RAF 批量写入。
 * - P8-09：全书搜索替换横条（300ms debounce，Esc 关闭）。
 * - P8-14：移动端响应式，侧栏改为底部抽屉，编辑区全宽显示。
 * - P8-18：渐进式用户引导（首次进入/首次选中/3000字里程碑）。
 * - P8-22：SnapshotDrawer 懒挂载（用户首次点击"历史"时才下载）。
 */
import {computed, defineAsyncComponent, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import type {Editor} from '@tiptap/vue-3'
import {NButton, NDrawer, NDrawerContent, NEmpty, NInput, NLayout, NLayoutContent, NSpace, NSpin, useMessage} from 'naive-ui'
import ChapterEditor from '@/components/ChapterEditor.vue'
import EditorSidePanel from '@/components/EditorSidePanel.vue'
import AiFloatButton from '@/components/AiFloatButton.vue'
import SearchReplaceBar from '@/components/SearchReplaceBar.vue'
import OnboardingHint from '@/components/OnboardingHint.vue'
import type {ChapterInfo, OutlineInfo} from '@/api/project'
import {getChapter, getOutline, saveChapter} from '@/api/project'
import {getWritingOverview} from '@/api/stats'
import {useDevice} from '@/composables/useDevice'
import {useCommandPaletteStore} from '@/stores/commandPalette'
import {useOnboarding} from '@/composables/useOnboarding'
import {useEditorHeartbeat} from '@/composables/useEditorHeartbeat'

/**
 * P8-22：SnapshotDrawer 组件级懒加载。
 * 用户首次点击"历史"按钮时，才下载 diff 相关代码。
 * 移动端通常不触发此操作，可节省首屏流量。
 */
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

// P8-08: AI 浮窗状态
const aiVisible = ref(false)
// P8-11: 版本快照抽屉
const showSnapshot = ref(false)
/** P8-22：快照抽屉是否曾经被打开过（懒挂载控制，false 时不渲染 SnapshotDrawer）。 */
const snapshotEverOpened = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const editorInstance = ref<any>(undefined)

// P8-09: 搜索替换横条
const showSearch = ref(false)
const showReplace = ref(false)

// P8-14: 移动端侧栏抽屉状态
const showMobileSidePanel = ref(false)

// P1-4：连续写作提醒条
const streakReminderVisible = ref(false)
const streakDays = ref(0)
const todayRemainingChars = ref(0)

// P8-18: 渐进式引导
const ob = useOnboarding()
/** 首次进入编辑器引导提示。 */
const showFirstEditorHint = ref(false)
/** 首次选中文字引导提示。 */
const showFirstAiSelectHint = ref(false)
/** 字数里程碑 3000 字提示。 */
const showMilestone3000 = ref(false)
/** 当前已计入的最大字数（防止里程碑重复触发）。 */
const maxTrackedChars = ref(0)

const projectId = route.params.projectId as string
const currentChapterId = computed(() => route.params.chapterId as string | undefined)

// P2-5：多标签页心跳检测（使用当前章节 ID，无章节时传空串）
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

  // P8-18：字数里程碑检测（3000 字，每次保存后检查）
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

/** P8-07 侧栏章节选中。 */
function handleSelectChapter(chapterId: string) {
  router.push(`/projects/${projectId}/editor/${chapterId}`)
}

/** P8-08 检测文本选中显示 AI 按钮。P8-18 首次选中文字触发引导提示。 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function handleEditorMounted(editor: any) {
  editorInstance.value = editor
  editor.on('selectionUpdate', ({ editor: e }: { editor: Editor }) => {
    const { from, to } = e.state.selection
    const hasSelection = from !== to
    aiVisible.value = hasSelection
    // P8-18：首次选中文字时展示 AI 浮窗使用说明
    if (hasSelection && ob.shouldShow('first-ai-select')) {
      showFirstAiSelectHint.value = true
    }
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

  // P1-4：加载连续写作提醒条
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
  } catch { /* 静默失败，不影响编辑器功能 */ }

  // P8-18：首次进入编辑器时展示使用提示
  if (ob.shouldShow('first-editor')) {
    showFirstEditorHint.value = true
  }

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
      action: () => { snapshotEverOpened.value = true; showSnapshot.value = true },
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

      <!-- P2-5：多标签页冲突警告 -->
      <div v-if="otherTabEditing" class="other-tab-warning">
        ⚠️ 另一个标签页正在编辑本章节，请注意避免内容冲突
      </div>

      <!-- P1-4：连续写作提醒条 -->
      <div v-if="streakReminderVisible" class="streak-reminder">
        📝 你已连续更新 {{ streakDays }} 天，今天还差 {{ todayRemainingChars }} 字
        <NButton text size="tiny" style="margin-left: 8px" @click="streakReminderVisible = false">×</NButton>
      </div>

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
          <!-- P8-22：点击"历史"后才触发 snapshotEverOpened，懒加载 SnapshotDrawer 组件 -->
          <NButton size="small" text title="版本快照与 diff（P8-11）"
                   @click="snapshotEverOpened = true; showSnapshot = true">🕐 历史</NButton>
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
        <!-- P8-18 首次进入编辑器引导提示（非阻塞，展示于编辑器上方）-->
        <div v-if="showFirstEditorHint || showFirstAiSelectHint || showMilestone3000"
             style="padding: 0 16px; flex-shrink: 0">
          <OnboardingHint
            v-if="showFirstEditorHint"
            icon="✏️"
            title="开始写作"
            description="在编辑器中尽情写作。使用 Ctrl+F 搜索，Ctrl+H 替换，🕐历史 查看版本快照。"
            action-label="我知道了"
            variant="info"
            @close="showFirstEditorHint = false; ob.markDone('first-editor')"
          />
          <OnboardingHint
            v-if="showFirstAiSelectHint"
            icon="🤖"
            title="AI 续写助手"
            description="选中文字后，会出现 AI 浮窗。点击可对选中内容进行 AI 续写或润色，生成的内容带有绿色标识，你可以选择接受或忽略。"
            action-label="明白了"
            variant="info"
            @close="showFirstAiSelectHint = false; ob.markDone('first-ai-select')"
          />
          <OnboardingHint
            v-if="showMilestone3000"
            icon="🎉"
            title="已写超过 3000 字！"
            description="太棒了！你已经写超过 3000 字，坚持每天的写作目标，你的故事正在成形！"
            variant="success"
            @close="showMilestone3000 = false; ob.markDone('char-milestone-3000')"
          />
        </div>

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

  <!--
    P8-11 版本快照抽屉（放在 NLayout 外以避免层叠问题）。
    P8-22：snapshotEverOpened 为 false 时不渲染（懒挂载），用户首次点击"历史"后才下载组件代码。
  -->
  <SnapshotDrawer
    v-if="snapshotEverOpened && currentChapterId"
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
/* ─── P2-5 多标签页冲突警告 ─── */
.other-tab-warning {
  background: #fff7e6;
  color: #d46b08;
  font-size: 13px;
  padding: 5px 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ffd591;
  flex-shrink: 0;
}

/* ─── P1-4 连续写作提醒条 ─── */
.streak-reminder {
  background: linear-gradient(90deg, #fef3c7, #fef9e0);
  color: #92400e;
  font-size: 13px;
  padding: 6px 16px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #fde68a;
  flex-shrink: 0;
}

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
