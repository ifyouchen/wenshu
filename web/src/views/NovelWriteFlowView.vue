<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BookOpenText, FilePlus2, Layers, Sparkles } from 'lucide-vue-next'
import { createChapter, createProject, createVolume, getOutline, listProjects } from '@/api/project'
import type { OutlineInfo } from '@/api/project'
import type { ProjectInfo } from '@/api/types'
import { submitSkeleton } from '@/api/novel'
import { getWriteState } from '@/api/workflow'
import { demoOutline, demoProjects } from '@/mocks/wenshu'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const projects = ref<ProjectInfo[]>([])
const selectedProjectId = ref('')
const outline = ref<OutlineInfo | null>(null)
const demoData = ref(false)
const loading = ref(false)
const skeletonTaskId = ref('')

const projectForm = reactive({ title: '', genre: '', synopsis: '', worldview: '' })
const volumeForm = reactive({ title: '第一卷', conflict: '' })
const chapterForm = reactive({ title: '第一章', outline: '' })

const selectedProject = computed(() => projects.value.find((item) => item.id === selectedProjectId.value) || null)
const firstChapter = computed(() => outline.value?.volumes.flatMap((volume) => volume.chapters)[0] || null)

onMounted(load)
watch(selectedProjectId, loadState)

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

async function load() {
  loading.value = true
  try {
    const res = await listProjects()
    projects.value = res.data.data
  } catch (error) {
    projects.value = demoProjects
    demoData.value = true
    toast.warning(messageOf(error, '后端不可用，已进入演示数据模式'))
  } finally {
    selectedProjectId.value = String(route.query.projectId || projects.value[0]?.id || '')
    loading.value = false
  }
}

async function loadState() {
  if (!selectedProjectId.value) {
    outline.value = null
    return
  }
  try {
    const res = await getWriteState(selectedProjectId.value)
    outline.value = res.data.data.outline
  } catch {
    try {
      const res = await getOutline(selectedProjectId.value)
      outline.value = res.data.data
    } catch {
      outline.value = demoOutline(selectedProjectId.value)
      demoData.value = true
    }
  }
}

async function createNewProject() {
  if (!projectForm.title.trim()) return
  try {
    const res = await createProject({
      title: projectForm.title.trim(),
      genre: projectForm.genre || undefined,
      synopsis: projectForm.synopsis || undefined,
      worldview: projectForm.worldview || undefined,
    })
    projects.value.unshift(res.data.data)
    selectedProjectId.value = res.data.data.id
    toast.success('作品已创建')
  } catch (error) {
    const local: ProjectInfo = {
      id: `local-project-${Date.now()}`,
      userId: 'demo-user',
      title: projectForm.title.trim(),
      genre: projectForm.genre || null,
      synopsis: projectForm.synopsis || null,
      worldview: projectForm.worldview || null,
      totalWords: 0,
      dailyCharGoal: 2000,
      status: 'draft',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    projects.value.unshift(local)
    selectedProjectId.value = local.id
    demoData.value = true
    toast.warning(messageOf(error, '后端不可用，已创建本地演示作品'))
  }
}

async function addVolume() {
  if (!selectedProjectId.value || !volumeForm.title.trim()) return
  try {
    await createVolume(selectedProjectId.value, { title: volumeForm.title.trim(), conflict: volumeForm.conflict })
    toast.success('卷已创建')
    await loadState()
  } catch {
    outline.value = outline.value || { volumes: [] }
    outline.value.volumes.push({
      id: `local-volume-${Date.now()}`,
      title: volumeForm.title,
      conflict: volumeForm.conflict,
      sortOrder: outline.value.volumes.length,
      chapters: [],
    })
    demoData.value = true
  }
}

async function addChapter(volumeId?: string) {
  const targetVolume = volumeId || outline.value?.volumes[0]?.id
  if (!targetVolume || !chapterForm.title.trim()) return
  try {
    const res = await createChapter(targetVolume, { title: chapterForm.title.trim(), outline: chapterForm.outline })
    toast.success('章节已创建')
    router.push(`/projects/${selectedProjectId.value}/editor/${res.data.data.id}`)
  } catch {
    const volume = outline.value?.volumes.find((item) => item.id === targetVolume)
    const id = `local-chapter-${Date.now()}`
    volume?.chapters.push({ id, title: chapterForm.title, outline: chapterForm.outline, wordCount: 0, status: 'pending' })
    demoData.value = true
    router.push(`/projects/${selectedProjectId.value}/editor/${id}`)
  }
}

async function generateSkeleton() {
  if (!selectedProject.value) return
  try {
    const res = await submitSkeleton({
      projectId: selectedProject.value.id,
      genre: selectedProject.value.genre || undefined,
      synopsis: selectedProject.value.synopsis || undefined,
      worldview: selectedProject.value.worldview || undefined,
      targetWords: 100000,
    })
    skeletonTaskId.value = res.data.data.taskId
    toast.success('骨架生成任务已提交')
  } catch (error) {
    toast.error(messageOf(error, 'AI 骨架暂不可用，请先手动建大纲'))
  }
}

function enterEditor() {
  if (selectedProject.value && firstChapter.value) {
    router.push(`/projects/${selectedProject.value.id}/editor/${firstChapter.value.id}`)
  }
}
</script>

<template>
  <div class="ws-page">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Write Novel</p>
        <h1>写小说</h1>
        <p>先有作品和章节，再进入编辑器写正文。AI 骨架是可选入口。</p>
        <p v-if="demoData" class="ws-hint">当前为演示数据模式，真实接口恢复后自动优先使用后端。</p>
      </div>
      <select v-model="selectedProjectId" class="ws-select">
        <option value="">选择作品</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.title }}</option>
      </select>
    </section>

    <section class="flow-grid two">
      <form class="ws-card ws-form" @submit.prevent="createNewProject">
        <h2><FilePlus2 :size="18" />新建作品</h2>
        <label class="ws-field"><span>作品名</span><input v-model="projectForm.title" class="ws-input" placeholder="例如：长夜将明"></label>
        <label class="ws-field"><span>类型</span><input v-model="projectForm.genre" class="ws-input" placeholder="玄幻 / 都市 / 悬疑 / 短剧"></label>
        <label class="ws-field"><span>简介</span><textarea v-model="projectForm.synopsis" class="ws-textarea" /></label>
        <label class="ws-field"><span>世界观</span><textarea v-model="projectForm.worldview" class="ws-textarea" /></label>
        <button class="ws-button ws-button--primary">创建作品</button>
      </form>

      <section class="ws-card ws-form">
        <h2><Sparkles :size="18" />AI 故事骨架</h2>
        <p class="ws-hint">可选。模型不可用时不影响你手动建大纲和写正文。</p>
        <button class="ws-button" type="button" :disabled="!selectedProject" @click="generateSkeleton">生成故事骨架</button>
        <p v-if="skeletonTaskId" class="ws-hint">任务已提交：{{ skeletonTaskId }}</p>
      </section>
    </section>

    <section class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2><Layers :size="18" />大纲与章节</h2>
          <p>{{ selectedProject?.title || '请选择或创建作品' }}</p>
        </div>
        <button class="ws-button ws-button--primary" type="button" :disabled="!firstChapter" @click="enterEditor">
          <BookOpenText :size="16" />
          进入最近章节
        </button>
      </div>

      <div class="flow-grid two">
        <form class="ws-form" @submit.prevent="addVolume">
          <label class="ws-field"><span>卷标题</span><input v-model="volumeForm.title" class="ws-input"></label>
          <label class="ws-field"><span>本卷冲突</span><input v-model="volumeForm.conflict" class="ws-input"></label>
          <button class="ws-button" type="submit" :disabled="!selectedProject">新增卷</button>
        </form>
        <form class="ws-form" @submit.prevent="addChapter()">
          <label class="ws-field"><span>章节标题</span><input v-model="chapterForm.title" class="ws-input"></label>
          <label class="ws-field"><span>章节梗概</span><input v-model="chapterForm.outline" class="ws-input"></label>
          <button class="ws-button" type="submit" :disabled="!outline?.volumes.length">新增章节并写作</button>
        </form>
      </div>

      <div class="outline-list">
        <article v-for="volume in outline?.volumes || []" :key="volume.id" class="ws-card outline-volume">
          <h3>{{ volume.title || '未命名卷' }}</h3>
          <p>{{ volume.conflict || '尚未填写核心冲突。' }}</p>
          <div class="chapter-list">
            <div v-for="chapter in volume.chapters" :key="chapter.id" class="chapter-row">
              <button type="button" @click="router.push(`/projects/${selectedProjectId}/editor/${chapter.id}`)">
                {{ chapter.title || '未命名章节' }}
              </button>
              <span>{{ chapter.wordCount || 0 }} 字</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <div v-if="loading" class="ws-empty overlay">加载中...</div>
  </div>
</template>
