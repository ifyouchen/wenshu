<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clapperboard, Play, RefreshCw } from 'lucide-vue-next'
import { convertScript, listDrafts } from '@/api/script'
import { getOutline, listProjects } from '@/api/project'
import type { OutlineInfo } from '@/api/project'
import type { ProjectInfo, ScriptDraftInfo } from '@/api/types'
import { getScriptState, type ChapterBrief } from '@/api/workflow'
import { demoDraft, demoOutline, demoProject, demoProjects } from '@/mocks/wenshu'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const loading = ref(false)
const converting = ref(false)
const degraded = ref('')
const projects = ref<ProjectInfo[]>([])
const selectedProjectId = ref('')
const outline = ref<OutlineInfo>(demoOutline())
const drafts = ref<ScriptDraftInfo[]>([])
const adaptableChapters = ref<ChapterBrief[]>([])

const convertForm = reactive({
  title: '',
  psychologyStrategy: 'action',
  range: 'all',
})

const selectedProject = computed(() => projects.value.find((item) => item.id === selectedProjectId.value) || null)
const latestDraft = computed(() => drafts.value[0] || null)
const selectedRangeText = computed(() => {
  if (convertForm.range === 'all') return '整部作品'
  const chapter = adaptableChapters.value.find((item) => item.id === convertForm.range)
  return chapter?.title || '选中章节'
})

onMounted(loadProjects)

watch(selectedProjectId, async (projectId) => {
  if (!projectId) return
  convertForm.title = `${selectedProject.value?.title || '小说'} 改编草稿`
  await loadState(projectId)
})

async function loadProjects() {
  loading.value = true
  degraded.value = ''
  try {
    const res = await listProjects()
    projects.value = res.data.data || []
  } catch (error) {
    projects.value = demoProjects
    degraded.value = '作品接口不可用，已启用演示数据模式。'
  } finally {
    if (!projects.value.length) {
      projects.value = demoProjects
      degraded.value ||= '当前还没有作品，先用演示项目展示改编流程。'
    }
    selectedProjectId.value = String(route.query.projectId || projects.value[0]?.id || demoProject.id)
    loading.value = false
  }
}

async function loadState(projectId: string) {
  try {
    const res = await getScriptState(projectId)
    outline.value = res.data.data.outline
    drafts.value = res.data.data.drafts
    adaptableChapters.value = res.data.data.adaptableChapters
  } catch (error) {
    try {
      const [outlineRes, draftsRes] = await Promise.all([getOutline(projectId), listDrafts(projectId)])
      outline.value = outlineRes.data.data
      drafts.value = draftsRes.data.data || []
      adaptableChapters.value = buildChapterList(outline.value)
      degraded.value ||= '改编聚合接口不可用，已降级为大纲和草稿接口。'
    } catch {
      outline.value = demoOutline(projectId)
      drafts.value = [demoDraft]
      adaptableChapters.value = buildChapterList(outline.value)
      degraded.value ||= '改编接口不可用，已启用演示数据模式。'
    }
  }
  if (!convertForm.range) convertForm.range = 'all'
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

async function submitConvert() {
  if (!selectedProjectId.value) {
    toast.warning('请先选择作品')
    return
  }
  converting.value = true
  try {
    const res = await convertScript({
      projectId: selectedProjectId.value,
      title: convertForm.title || `${selectedProject.value?.title || '小说'} 改编草稿`,
      psychologyStrategy: convertForm.psychologyStrategy,
      chapterIds: convertForm.range === 'all' ? [] : [convertForm.range],
    })
    const draftId = res.data.data.draftId
    toast.success('改编任务已提交')
    router.push(`/projects/${selectedProjectId.value}/script/${draftId}`)
  } catch (error) {
    degraded.value = '改编接口或模型不可用，已进入演示草稿；真实生成请检查后端与模型配置。'
    router.push(`/projects/${selectedProjectId.value || demoProject.id}/script/${demoDraft.id}`)
  } finally {
    converting.value = false
  }
}

function openDraft(draft: ScriptDraftInfo) {
  router.push(`/projects/${draft.projectId || selectedProjectId.value}/script/${draft.id}`)
}
</script>

<template>
  <div class="ws-container flow-page">
    <section class="flow-hero">
      <div>
        <p class="ws-kicker">小说改剧本</p>
        <h1>先确定作品和改编范围，再进入四栏剧本工作台</h1>
        <p>流程是：选作品 → 选范围 → 提交改编 → 校订场景 → 导出。移动端先做选择，四栏校订建议在桌面完成。</p>
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
            <h2>改编设置</h2>
            <p>选择作品、范围和短剧化策略。</p>
          </div>
        </div>

        <div class="form-stack">
          <label class="ws-field">
            <span>作品</span>
            <select v-model="selectedProjectId" class="ws-select">
              <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.title }}</option>
            </select>
          </label>
          <label class="ws-field">
            <span>范围</span>
            <select v-model="convertForm.range" class="ws-select">
              <option value="all">整部作品</option>
              <option v-for="chapter in adaptableChapters" :key="chapter.id" :value="chapter.id">
                {{ chapter.volumeTitle }} / {{ chapter.title }}
              </option>
            </select>
          </label>
          <label class="ws-field">
            <span>草稿标题</span>
            <input v-model="convertForm.title" class="ws-input" />
          </label>
          <label class="ws-field">
            <span>改编策略</span>
            <select v-model="convertForm.psychologyStrategy" class="ws-select">
              <option value="action">动作推进</option>
              <option value="dialogue">台词张力</option>
              <option value="emotion">情绪拉扯</option>
            </select>
          </label>
          <button class="ws-btn ws-btn--primary" type="button" :disabled="converting" @click="submitConvert">
            <Play :size="16" />{{ converting ? '提交中' : '生成剧本草稿' }}
          </button>
        </div>
      </div>

      <div class="ws-card">
        <div class="ws-section-title">
          <div>
            <h2>本次改编</h2>
            <p>{{ selectedProject?.title || '未选择作品' }}</p>
          </div>
        </div>
        <div class="script-summary">
          <Clapperboard :size="34" />
          <strong>{{ selectedRangeText }}</strong>
          <span>{{ adaptableChapters.length }} 个章节可作为改编来源</span>
        </div>
        <div class="chapter-picker compact">
          <button
            v-for="draft in drafts"
            :key="draft.id"
            class="chapter-option"
            type="button"
            @click="openDraft(draft)"
          >
            <strong>{{ draft.title || '未命名草稿' }}</strong>
            <span>{{ draft.status }} · {{ draft.totalScenes || 0 }} 场</span>
          </button>
          <div v-if="!drafts.length" class="ws-empty">还没有剧本草稿，先生成一个。</div>
        </div>
      </div>
    </section>

    <section class="ws-card">
      <div class="ws-section-title">
        <div>
          <h2>改编来源预览</h2>
          <p>大纲会作为转换上下文的一部分；场景校订在下一步完成。</p>
        </div>
        <button v-if="latestDraft" class="ws-btn ws-btn--secondary" type="button" @click="openDraft(latestDraft)">
          继续校订
        </button>
      </div>
      <div class="outline-strip">
        <article v-for="volume in outline.volumes" :key="volume.id">
          <strong>{{ volume.title || '未命名卷' }}</strong>
          <span>{{ volume.conflict || '暂无卷冲突描述' }}</span>
          <small>{{ volume.chapters.length }} 章</small>
        </article>
      </div>
    </section>
  </div>
</template>
