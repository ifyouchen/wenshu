<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle2, FileInput, RefreshCw, Wand2 } from 'lucide-vue-next'
import { submitConsistencyCheck } from '@/api/consistency'
import { pasteImport } from '@/api/import'
import { getChapter, getOutline, listProjects } from '@/api/project'
import type { ChapterInfo, OutlineInfo } from '@/api/project'
import { applyRewrite, rewriteChapter, type RewriteMode, type RewriteSuggestion } from '@/api/rewrite'
import type { ProjectInfo } from '@/api/types'
import { getRewriteState, type ChapterBrief } from '@/api/workflow'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const loading = ref(false)
const degraded = ref('')
const projects = ref<ProjectInfo[]>([])
const selectedProjectId = ref('')
const outline = ref<OutlineInfo | null>(null)
const chapters = ref<ChapterBrief[]>([])
const selectedChapterId = ref('')
const currentChapter = ref<ChapterInfo | null>(null)
const suggestion = ref<RewriteSuggestion | null>(null)
const running = ref(false)
const sourceMode = ref<'chapter' | 'paste'>('chapter')

const rewriteForm = reactive({
  mode: 'polish' as RewriteMode,
  instruction: '',
})

const importForm = reactive({
  title: '导入改稿',
  text: '',
})

const selectedProject = computed(() => projects.value.find((item) => item.id === selectedProjectId.value) || null)
const chapterSource = computed(() => currentChapter.value?.content || '')
const plainSource = computed(() => stripHtml(chapterSource.value))
const activeVolumeId = computed(() => outline.value?.volumes[0]?.id || '')

onMounted(loadProjects)

watch(selectedProjectId, async (projectId) => {
  if (projectId) await loadState(projectId)
})

watch(selectedChapterId, async (chapterId) => {
  if (chapterId) await loadChapter(chapterId)
})

function stripHtml(html: string) {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

function buildChapterList(nextOutline: OutlineInfo): ChapterBrief[] {
  return nextOutline.volumes.flatMap((volume) =>
    volume.chapters.map((chapter) => ({
      id: chapter.id,
      title: chapter.title,
      volumeTitle: volume.title,
      wordCount: chapter.wordCount,
      status: chapter.status,
    })),
  )
}

async function loadProjects() {
  loading.value = true
  degraded.value = ''
  try {
    const res = await listProjects()
    projects.value = res.data.data || []
  } catch (error) {
    projects.value = []
    degraded.value = messageOf(error, '作品接口不可用，请确认后端服务和登录状态。')
    toast.error(degraded.value)
  } finally {
    if (!projects.value.length) {
      degraded.value ||= '当前还没有作品，请先创建作品后再改稿。'
    }
    selectedProjectId.value = String(route.query.projectId || projects.value[0]?.id || '')
    loading.value = false
  }
}

async function loadState(projectId: string) {
  suggestion.value = null
  degraded.value = ''
  try {
    const res = await getRewriteState(projectId)
    outline.value = res.data.data.outline
    chapters.value = res.data.data.chapters
  } catch (error) {
    try {
      const res = await getOutline(projectId)
      outline.value = res.data.data
      chapters.value = buildChapterList(outline.value)
      degraded.value ||= '改稿聚合接口不可用，已降级为大纲接口。'
    } catch (fallbackError) {
      outline.value = null
      chapters.value = []
      degraded.value = messageOf(fallbackError, '改稿接口不可用，请稍后重试。')
      toast.error(degraded.value)
    }
  }
  selectedChapterId.value = String(route.query.chapterId || chapters.value[0]?.id || '')
}

async function loadChapter(chapterId: string) {
  suggestion.value = null
  try {
    const res = await getChapter(chapterId)
    currentChapter.value = res.data.data
  } catch (error) {
    currentChapter.value = null
    degraded.value = messageOf(error, '章节详情加载失败，请稍后重试。')
    toast.error(degraded.value)
  }
}

async function importPastedText() {
  if (!selectedProjectId.value || !importForm.text.trim()) {
    toast.warning('请先选择作品并粘贴需要改稿的正文')
    return
  }
  try {
    if (!activeVolumeId.value) throw new Error('缺少可导入卷')
    const res = await pasteImport(selectedProjectId.value, activeVolumeId.value, importForm.text.trim())
    const chapter = res.data.data?.[0]
    if (!chapter) throw new Error('导入后未返回章节')
    await loadState(selectedProjectId.value)
    selectedChapterId.value = chapter.id
    sourceMode.value = 'chapter'
    toast.success('已导入为章节，可以开始改稿')
  } catch (error) {
    degraded.value = messageOf(error, '导入失败，请确认作品中已有卷并稍后重试。')
    toast.error(degraded.value)
  }
}

async function runRewrite() {
  if (!currentChapter.value) {
    toast.warning('请先选择或导入一章正文')
    return
  }
  running.value = true
  suggestion.value = null
  try {
    const res = await rewriteChapter(currentChapter.value.id, {
      mode: rewriteForm.mode,
      instruction: rewriteForm.instruction,
      selectedText: plainSource.value,
    })
    suggestion.value = res.data.data
  } catch (error) {
    degraded.value = messageOf(error, '改稿接口或模型不可用，请检查后端与模型配置。')
    toast.error(degraded.value)
  } finally {
    running.value = false
  }
}

async function applySuggestion() {
  if (!currentChapter.value || !suggestion.value) return
  try {
    const content = `<p>${suggestion.value.rewritten.replace(/\n+/g, '</p><p>')}</p>`
    const res = await applyRewrite(currentChapter.value.id, {
      content,
      acceptedChars: suggestion.value.rewritten.length,
      snapshotLabel: `改稿应用 - ${rewriteForm.mode}`,
    })
    currentChapter.value = res.data.data
    toast.success('已应用修改并创建快照')
  } catch (error) {
    degraded.value = messageOf(error, '应用修改失败，请稍后重试。')
    toast.error(degraded.value)
  }
}

async function checkConsistency() {
  if (!selectedProjectId.value) return
  try {
    const res = await submitConsistencyCheck(selectedProjectId.value)
    router.push(`/consistency/reports/${res.data.data.reportId}`)
  } catch (error) {
    degraded.value = '一致性检查暂不可用，请确认模型配置或后端服务。'
    toast.warning('一致性检查暂不可用')
  }
}
</script>

<template>
  <div class="ws-container flow-page">
    <section class="flow-hero">
      <div>
        <p class="ws-kicker">改小说</p>
        <h1>选择已有章节，或把稿件导入后直接改</h1>
        <p>流程是：选来源 → 选择改稿方式 → 预览建议 → 应用到章节。接口不可用时会明确降级，不再让你卡在空页面。</p>
      </div>
      <button class="ws-btn ws-btn--secondary" type="button" :disabled="loading" @click="loadProjects">
        <RefreshCw :size="16" />刷新
      </button>
    </section>

    <div v-if="degraded" class="ws-alert ws-alert--warning">{{ degraded }}</div>

    <section class="flow-grid two">
      <div class="ws-card">
        <div class="ws-section-title">
          <div>
            <h2>稿件来源</h2>
            <p>可以选择作品里的章节，也可以粘贴一段新稿。</p>
          </div>
        </div>

        <label class="ws-field">
          <span>作品</span>
          <select v-model="selectedProjectId" class="ws-select">
            <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.title }}</option>
          </select>
        </label>

        <div class="segmented">
          <button :class="{ active: sourceMode === 'chapter' }" type="button" @click="sourceMode = 'chapter'">已有章节</button>
          <button :class="{ active: sourceMode === 'paste' }" type="button" @click="sourceMode = 'paste'">粘贴导入</button>
        </div>

        <div v-if="sourceMode === 'chapter'" class="chapter-picker">
          <button
            v-for="chapter in chapters"
            :key="chapter.id"
            class="chapter-option"
            :class="{ active: selectedChapterId === chapter.id }"
            type="button"
            @click="selectedChapterId = chapter.id"
          >
            <strong>{{ chapter.title || '未命名章节' }}</strong>
            <span>{{ chapter.volumeTitle || '未分卷' }} · {{ chapter.wordCount }} 字</span>
          </button>
        </div>

        <div v-else class="form-stack">
          <label class="ws-field">
            <span>标题</span>
            <input v-model="importForm.title" class="ws-input" />
          </label>
          <label class="ws-field">
            <span>粘贴正文</span>
            <textarea v-model="importForm.text" class="ws-textarea tall" placeholder="把需要修改的小说正文粘贴到这里"></textarea>
          </label>
          <button class="ws-btn ws-btn--primary" type="button" @click="importPastedText">
            <FileInput :size="16" />导入并改稿
          </button>
        </div>
      </div>

      <div class="ws-card rewrite-preview">
        <div class="ws-section-title">
          <div>
            <h2>原文</h2>
            <p>{{ selectedProject?.title || '未选择作品' }} / {{ currentChapter?.title || '未选择章节' }}</p>
          </div>
        </div>
        <div class="source-pane" v-html="chapterSource || '<p>请选择章节或导入稿件。</p>'"></div>
      </div>
    </section>

    <section class="flow-grid two">
      <div class="ws-card">
        <div class="ws-section-title">
          <div>
            <h2>改稿指令</h2>
            <p>常用模式走真实接口，失败时会显示后端错误。</p>
          </div>
        </div>
        <label class="ws-field">
          <span>方式</span>
          <select v-model="rewriteForm.mode" class="ws-select">
            <option value="polish">润色</option>
            <option value="expand">扩写</option>
            <option value="shorten">缩写</option>
            <option value="custom">自定义</option>
          </select>
        </label>
        <label class="ws-field">
          <span>补充要求</span>
          <textarea v-model="rewriteForm.instruction" class="ws-textarea" placeholder="例如：增强悬疑感，减少解释性句子"></textarea>
        </label>
        <div class="button-row">
          <button class="ws-btn ws-btn--primary" type="button" :disabled="running" @click="runRewrite">
            <Wand2 :size="16" />{{ running ? '生成中' : '生成改稿建议' }}
          </button>
          <button class="ws-btn ws-btn--secondary" type="button" @click="checkConsistency">
            一致性检查
          </button>
        </div>
      </div>

      <div class="ws-card rewrite-preview">
        <div class="ws-section-title">
          <div>
            <h2>AI 建议</h2>
            <p>应用前不会覆盖原文。</p>
          </div>
          <button class="ws-btn ws-btn--primary" type="button" :disabled="!suggestion" @click="applySuggestion">
            <CheckCircle2 :size="16" />应用修改
          </button>
        </div>
        <pre class="suggestion-pane">{{ suggestion?.rewritten || '改稿结果会显示在这里。' }}</pre>
      </div>
    </section>
  </div>
</template>
