<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpenText, Clapperboard, FilePenLine, Play, RefreshCw } from 'lucide-vue-next'
import { getDashboard, type DashboardState } from '@/api/workflow'
import { listProjects } from '@/api/project'
import type { ProjectInfo } from '@/api/types'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()
const loading = ref(false)
const loadError = ref('')
const dashboard = ref<DashboardState | null>(null)
const projects = ref<ProjectInfo[]>([])

const recentProject = computed(() => dashboard.value?.recentProjects?.[0] || projects.value[0] || null)
const continueChapter = computed(() => dashboard.value?.continueChapter || null)
const recentDraft = computed(() => dashboard.value?.recentScriptDraft || null)

onMounted(loadHome)

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

async function loadHome() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getDashboard()
    dashboard.value = res.data.data
    projects.value = res.data.data.recentProjects
  } catch (error) {
    try {
      const res = await listProjects()
      projects.value = res.data.data
      loadError.value = messageOf(error, '工作台聚合接口暂不可用，已展示作品列表。')
    } catch {
      projects.value = []
      loadError.value = messageOf(error, '工作台加载失败，请确认后端服务和登录状态。')
      toast.error(loadError.value)
    }
  } finally {
    loading.value = false
  }
}

function goContinueWrite() {
  const project = recentProject.value
  const chapter = continueChapter.value
  if (project && chapter) router.push(`/projects/${project.id}/editor/${chapter.id}`)
  else router.push('/write')
}
</script>

<template>
  <div class="ws-page flow-home">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Wenshu Workflow</p>
        <h1>你今天要做什么？</h1>
        <p>按创作目标进入流程：写小说、改小说、小说改剧本。</p>
        <p v-if="loadError" class="ws-hint">{{ loadError }}</p>
      </div>
      <button class="ws-button" type="button" :disabled="loading" @click="loadHome">
        <RefreshCw :size="16" />
        刷新
      </button>
    </section>

    <section class="quick-actions">
      <button class="quick-action primary" type="button" @click="goContinueWrite">
        <Play :size="18" />
        <span>
          <strong>继续写作</strong>
          <small>{{ recentProject?.title || '从新作品开始' }}</small>
        </span>
      </button>
      <button class="quick-action" type="button" @click="router.push('/rewrite')">
        <FilePenLine :size="18" />
        <span>
          <strong>导入/选择章节改稿</strong>
          <small>润色、扩写、缩写、查一致性</small>
        </span>
      </button>
      <button class="quick-action" type="button" @click="router.push('/script-flow')">
        <Clapperboard :size="18" />
        <span>
          <strong>小说改剧本</strong>
          <small>{{ recentDraft?.title || '选择作品生成剧本' }}</small>
        </span>
      </button>
    </section>

    <section class="mainline-grid">
      <article class="mainline-card">
        <BookOpenText :size="30" />
        <span class="ws-badge">主线 1</span>
        <h2>写小说</h2>
        <p>创建作品，整理大纲和角色，进入章节编辑器写正文。AI 骨架是可选入口，不会阻塞手动写作。</p>
        <ol>
          <li>新建作品或选择已有作品</li>
          <li>创建卷和章节</li>
          <li>进入编辑器续写、润色、保存</li>
        </ol>
        <button class="ws-button ws-button--primary" type="button" @click="router.push('/write')">开始写小说</button>
      </article>

      <article class="mainline-card">
        <FilePenLine :size="30" />
        <span class="ws-badge">主线 2</span>
        <h2>改小说</h2>
        <p>选择已有章节，或粘贴/上传旧稿；生成改稿建议后人工确认应用，避免 AI 直接覆盖正文。</p>
        <ol>
          <li>选择章节或导入稿件</li>
          <li>选择润色、扩写、缩写或自定义要求</li>
          <li>预览结果，确认后应用并生成快照</li>
        </ol>
        <button class="ws-button ws-button--primary" type="button" @click="router.push('/rewrite')">开始改小说</button>
      </article>

      <article class="mainline-card">
        <Clapperboard :size="30" />
        <span class="ws-badge">主线 3</span>
        <h2>小说改剧本</h2>
        <p>选择作品生成剧本草稿，再进入四栏工作台逐场校订：场景、原文、剧本、笔记。</p>
        <ol>
          <li>选择作品和改编策略</li>
          <li>提交剧本改编任务</li>
          <li>逐场校订并导出 DOCX/FDX/分镜表</li>
        </ol>
        <button class="ws-button ws-button--primary" type="button" @click="router.push('/script-flow')">开始改剧本</button>
      </article>
    </section>

    <section class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2>最近作品</h2>
          <p>作品资料、角色库、词典和世界观会在具体流程内作为上下文使用。</p>
        </div>
        <button class="ws-button" type="button" @click="router.push('/write')">管理作品</button>
      </div>
      <div v-if="!projects.length" class="ws-empty compact">暂无作品，先从“写小说”创建一个。</div>
      <div v-else class="recent-list">
        <article v-for="project in projects.slice(0, 4)" :key="project.id" class="recent-item">
          <strong>{{ project.title }}</strong>
          <span>{{ project.genre || '未分类' }} · {{ project.totalWords || 0 }} 字</span>
          <div class="ws-actions">
            <button class="ws-button" type="button" @click="router.push(`/write?projectId=${project.id}`)">写</button>
            <button class="ws-button" type="button" @click="router.push(`/rewrite?projectId=${project.id}`)">改</button>
            <button class="ws-button" type="button" @click="router.push(`/script-flow?projectId=${project.id}`)">剧本</button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
