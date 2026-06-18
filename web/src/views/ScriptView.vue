<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FileDown, Save } from 'lucide-vue-next'
import { convertScript, exportDraft, getDraft, listDrafts, listScenes, updateScene } from '@/api/script'
import type { ScriptDraftInfo, ScriptSceneInfo } from '@/api/types'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const projectId = computed(() => String(route.params.projectId))
const draftId = computed(() => String(route.params.draftId || ''))

const loading = ref(false)
const converting = ref(false)
const saving = ref(false)
const drafts = ref<ScriptDraftInfo[]>([])
const draft = ref<ScriptDraftInfo | null>(null)
const scenes = ref<ScriptSceneInfo[]>([])
const selectedSceneId = ref('')
const content = ref('')
const exportFormat = ref<'docx' | 'fdx' | 'storyboard'>('docx')

const selectedScene = computed(() => scenes.value.find((item) => item.id === selectedSceneId.value) || null)

onMounted(load)
watch(draftId, load)

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

async function load() {
  loading.value = true
  try {
    if (!draftId.value) {
      try {
        const res = await listDrafts(projectId.value)
        drafts.value = res.data.data
      } catch (error) {
        drafts.value = []
        toast.error(messageOf(error, '剧本草稿列表加载失败'))
      }
      return
    }
    try {
      const [draftRes, sceneRes] = await Promise.all([getDraft(draftId.value), listScenes(draftId.value, 0, 200)])
      draft.value = draftRes.data.data
      scenes.value = sceneRes.data.data.scenes
    } catch (error) {
      draft.value = null
      scenes.value = []
      toast.error(messageOf(error, '剧本工作台加载失败'))
    }
    if (scenes.value[0]) selectScene(scenes.value[0])
  } catch (error) {
    toast.error(messageOf(error, '剧本工作台加载失败'))
  } finally {
    loading.value = false
  }
}

function selectScene(scene: ScriptSceneInfo) {
  selectedSceneId.value = scene.id
  content.value = scene.content || ''
}

async function startConvert() {
  converting.value = true
  try {
    const res = await convertScript({ projectId: projectId.value, title: '剧本改编草稿', psychologyStrategy: 'action' })
    router.push(`/projects/${projectId.value}/script/${res.data.data.draftId}`)
  } catch (error) {
    toast.error(messageOf(error, '改编接口或模型不可用，请检查后端与模型配置。'))
  } finally {
    converting.value = false
  }
}

async function saveScene() {
  if (!selectedScene.value) return
  saving.value = true
  try {
    const res = await updateScene(selectedScene.value.id, {
      content: content.value,
      location: selectedScene.value.location || undefined,
      timeDesc: selectedScene.value.timeDesc || undefined,
      version: selectedScene.value.version,
    })
    Object.assign(selectedScene.value, res.data.data)
    toast.success('场景已保存')
  } catch (error) {
    toast.error(messageOf(error, '保存失败，可能存在版本冲突'))
  } finally {
    saving.value = false
  }
}

async function submitExport() {
  if (!draft.value) return
  await exportDraft(draft.value.id, exportFormat.value)
  toast.success('导出任务已提交')
}
</script>

<template>
  <div class="script-shell">
    <div class="script-mobile">剧本工作台需要桌面空间，请在电脑端使用完整四栏视图。</div>

    <section v-if="!draftId" class="ws-page draft-entry">
      <button class="ws-button" type="button" @click="router.push(`/projects/${projectId}/editor`)">
        <ArrowLeft :size="16" />
        返回编辑器
      </button>
      <div class="ws-page__head">
        <div>
          <p class="ws-eyebrow">Script</p>
          <h1>剧本改编</h1>
          <p>从小说正文生成剧本草稿，再逐场校订和导出。</p>
        </div>
        <button class="ws-button ws-button--primary" :disabled="converting" type="button" @click="startConvert">
          {{ converting ? '提交中' : '发起改编' }}
        </button>
      </div>
      <div class="ws-grid">
        <article v-for="item in drafts" :key="item.id" class="ws-card compact-card">
          <h3>{{ item.title || '未命名剧本草稿' }}</h3>
          <p>{{ item.totalScenes || 0 }} 场 · {{ item.status }}</p>
          <button class="ws-button" type="button" @click="router.push(`/projects/${projectId}/script/${item.id}`)">打开</button>
        </article>
      </div>
    </section>

    <template v-else>
      <header class="script-bar">
        <button class="ws-button" type="button" @click="router.push(`/projects/${projectId}/editor`)">
          <ArrowLeft :size="16" />
          正文
        </button>
        <strong>{{ draft?.title || '剧本工作台' }}</strong>
        <select v-model="exportFormat" class="ws-select">
          <option value="docx">DOCX</option>
          <option value="fdx">FDX</option>
          <option value="storyboard">分镜表</option>
        </select>
        <button class="ws-button" type="button" @click="submitExport"><FileDown :size="16" />导出</button>
        <button class="ws-button ws-button--primary" :disabled="saving" type="button" @click="saveScene"><Save :size="16" />保存</button>
      </header>

      <main class="script-grid">
        <aside class="script-pane scene-pane">
          <header><span>场景</span></header>
          <button
            v-for="scene in scenes"
            :key="scene.id"
            class="scene-item"
            :class="{ active: scene.id === selectedSceneId }"
            type="button"
            @click="selectScene(scene)"
          >
            <strong>场 {{ scene.sceneIndex + 1 }}</strong>
            <span>{{ scene.location || '未标地点' }} · {{ scene.timeDesc || '时间待定' }}</span>
          </button>
        </aside>
        <section class="script-pane">
          <header>小说原文</header>
          <div class="pane-body">{{ selectedScene?.sourceContent || '请选择场景，或先等待改编任务生成场景。' }}</div>
        </section>
        <section class="script-pane script-output">
          <header>剧本输出</header>
          <textarea v-model="content" class="pane-editor" placeholder="在这里校订剧本内容" />
        </section>
      </main>
    </template>

    <div v-if="loading" class="ws-empty overlay">加载中...</div>
  </div>
</template>
