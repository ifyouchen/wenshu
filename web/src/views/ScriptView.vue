<script setup lang="ts">
/**
 * 剧本改编工作台：场景目录、原文、剧本编辑、改编建议。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NEmpty,
  NIcon,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import {
  AlertTriangle,
  ArrowLeft,
  FileDown,
  FileText,
  Layers,
  Plus,
  Save,
  ScrollText,
  Trash2,
} from 'lucide-vue-next'
import {
  convertScript,
  createEpisode,
  deleteEpisode,
  exportDraft,
  getDraft,
  listDrafts,
  listEpisodes,
  listScenes,
  updateScene,
} from '@/api/script'
import type { ScriptDraftInfo, ScriptEpisodeInfo, ScriptSceneInfo } from '@/api/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const projectId = computed(() => route.params.projectId as string)
const draftId = computed(() => route.params.draftId as string | undefined)

const drafts = ref<ScriptDraftInfo[]>([])
const draft = ref<ScriptDraftInfo | null>(null)
const scenes = ref<ScriptSceneInfo[]>([])
const episodes = ref<ScriptEpisodeInfo[]>([])
const selectedSceneId = ref('')
const editContent = ref('')
const loading = ref(false)
const saving = ref(false)
const converting = ref(false)
const exporting = ref(false)
const showConflict = ref(false)
const conflictLocalText = ref('')
const newEpisodeTitle = ref('')
const exportFormat = ref<'docx' | 'fdx' | 'storyboard'>('docx')

const selectedScene = computed(() => scenes.value.find(scene => scene.id === selectedSceneId.value) ?? null)
const currentEpisode = computed(() => episodes.value[0] ?? null)

const exportOptions = [
  { label: '中文影视标准 DOCX', value: 'docx' },
  { label: 'Final Draft FDX', value: 'fdx' },
  { label: '简单分镜表', value: 'storyboard' },
]

async function loadDraftList() {
  loading.value = true
  try {
    const res = await listDrafts(projectId.value)
    drafts.value = res.data.data
  } catch {
    message.error('剧本草稿加载失败')
  } finally {
    loading.value = false
  }
}

async function loadWorkbench(id: string) {
  loading.value = true
  try {
    const [draftRes, sceneRes, episodeRes] = await Promise.all([
      getDraft(id),
      listScenes(id, 0, 100),
      listEpisodes(id),
    ])
    draft.value = draftRes.data.data
    scenes.value = sceneRes.data.data.scenes
    episodes.value = episodeRes.data.data
    if (scenes.value.length) selectScene(scenes.value[0])
  } catch {
    message.error('工作台加载失败')
  } finally {
    loading.value = false
  }
}

function selectScene(scene: ScriptSceneInfo) {
  selectedSceneId.value = scene.id
  editContent.value = scene.content ?? ''
}

async function saveScene() {
  if (!selectedScene.value) return
  saving.value = true
  try {
    const res = await updateScene(selectedScene.value.id, {
      content: editContent.value,
      location: selectedScene.value.location ?? undefined,
      timeDesc: selectedScene.value.timeDesc ?? undefined,
      version: selectedScene.value.version,
    })
    const updated = res.data.data
    const idx = scenes.value.findIndex(scene => scene.id === updated.id)
    if (idx !== -1) scenes.value[idx] = { ...scenes.value[idx], ...updated }
    selectedSceneId.value = updated.id
    message.success('场景已保存')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 409) {
      conflictLocalText.value = editContent.value
      showConflict.value = true
    } else {
      message.error('保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

async function startConversion() {
  converting.value = true
  try {
    const res = await convertScript({
      projectId: projectId.value,
      title: '剧本改编草稿',
      psychologyStrategy: 'action',
    })
    message.success('改编任务已提交，正在进入工作台')
    router.push(`/projects/${projectId.value}/script/${res.data.data.draftId}`)
  } catch {
    message.error('改编任务提交失败')
  } finally {
    converting.value = false
  }
}

async function handleExport() {
  if (!draft.value) return
  exporting.value = true
  try {
    await exportDraft(draft.value.id, exportFormat.value)
    message.success('导出任务已提交，完成后可在任务进度中获取文件')
  } catch {
    message.error('导出任务提交失败')
  } finally {
    exporting.value = false
  }
}

async function handleCreateEpisode() {
  if (!draft.value || !newEpisodeTitle.value.trim()) return
  try {
    const res = await createEpisode(draft.value.id, {
      episodeNo: episodes.value.length + 1,
      title: newEpisodeTitle.value.trim(),
    })
    episodes.value.push(res.data.data)
    newEpisodeTitle.value = ''
    message.success('分集已创建')
  } catch {
    message.error('分集创建失败')
  }
}

async function handleDeleteEpisode(episode: ScriptEpisodeInfo) {
  if (!draft.value) return
  try {
    await deleteEpisode(draft.value.id, episode.id)
    episodes.value = episodes.value.filter(item => item.id !== episode.id)
    message.success('分集已删除')
  } catch {
    message.error('删除分集失败')
  }
}

function discardLocalConflict() {
  if (selectedScene.value) {
    editContent.value = selectedScene.value.content ?? ''
  }
  showConflict.value = false
  message.info('已保留服务器版本')
}

onMounted(async () => {
  if (draftId.value) await loadWorkbench(draftId.value)
  else await loadDraftList()
})

watch(draftId, async (id) => {
  if (id) await loadWorkbench(id)
  else await loadDraftList()
})
</script>

<template>
  <div class="script-page">
    <div class="mobile-hint">
      <NIcon :component="AlertTriangle" :size="18" />
      <span>剧本工作台需要桌面端空间。请在电脑上打开以获得完整四栏视图。</span>
    </div>

    <div v-if="loading" class="script-loading">
      <NSpin size="large" />
    </div>

    <section v-else-if="!draftId" class="draft-entry">
      <button class="back-link" @click="router.push(`/projects/${projectId}/editor`)">
        <NIcon :component="ArrowLeft" :size="16" />
        返回编辑器
      </button>
      <div class="draft-entry-head">
        <p class="w-eyebrow">Script</p>
        <h1 class="w-title">剧本改编</h1>
        <p class="w-subtitle">从小说正文进入剧本草稿，再逐场校订和导出。</p>
      </div>

      <div class="draft-entry-grid">
        <button class="draft-start-card" :disabled="converting" @click="startConversion">
          <NIcon :component="ScrollText" :size="28" />
          <strong>{{ converting ? '正在提交改编任务' : '发起新的改编' }}</strong>
          <span>默认使用动作外化策略，生成后可在工作台逐场调整。</span>
        </button>

        <div class="draft-list-card">
          <div class="draft-list-head">
            <strong>已有草稿</strong>
            <NTag size="small" :bordered="false">{{ drafts.length }} 个</NTag>
          </div>
          <div v-if="drafts.length" class="draft-list">
            <button
              v-for="item in drafts"
              :key="item.id"
              class="draft-item"
              @click="router.push(`/projects/${projectId}/script/${item.id}`)"
            >
              <span>
                <strong>{{ item.title || '未命名剧本草稿' }}</strong>
                <small>{{ item.totalScenes ?? 0 }} 场 · {{ item.status }}</small>
              </span>
              <NIcon :component="ArrowLeft" :size="15" class="draft-arrow" />
            </button>
          </div>
          <NEmpty v-else description="暂无剧本草稿" />
        </div>
      </div>
    </section>

    <template v-else>
      <header class="script-topbar">
        <button class="back-link" @click="router.push(`/projects/${projectId}/editor`)">
          <NIcon :component="ArrowLeft" :size="16" />
          返回正文
        </button>
        <div class="script-title">
          <strong>{{ draft?.title || '剧本工作台' }}</strong>
          <span>{{ scenes.length }} 场 · {{ draft?.status || '草稿' }}</span>
        </div>
        <div class="script-actions">
          <NSelect v-model:value="exportFormat" :options="exportOptions" size="small" class="export-select" />
          <NButton size="small" secondary :loading="exporting" @click="handleExport">
            <template #icon>
              <NIcon :component="FileDown" :size="14" />
            </template>
            导出
          </NButton>
          <NButton size="small" type="primary" :loading="saving" :disabled="!selectedScene" @click="saveScene">
            <template #icon>
              <NIcon :component="Save" :size="14" />
            </template>
            保存场景
          </NButton>
        </div>
      </header>

      <main class="script-workbench">
        <aside class="scene-column">
          <div class="column-head">
            <NIcon :component="Layers" :size="15" />
            <span>场景目录</span>
          </div>
          <div class="episode-box">
            <div class="episode-title">
              <strong>{{ currentEpisode?.title || '默认分集' }}</strong>
              <small>{{ episodes.length || 1 }} 集</small>
            </div>
            <div class="episode-create">
              <NInput v-model:value="newEpisodeTitle" size="small" placeholder="新分集标题" />
              <NButton size="small" secondary @click="handleCreateEpisode">
                <template #icon>
                  <NIcon :component="Plus" :size="13" />
                </template>
              </NButton>
            </div>
            <button
              v-for="episode in episodes"
              :key="episode.id"
              class="episode-chip"
              @click="handleDeleteEpisode(episode)"
            >
              <span>{{ episode.title }}</span>
              <NIcon :component="Trash2" :size="12" />
            </button>
          </div>
          <div class="scene-list">
            <button
              v-for="scene in scenes"
              :key="scene.id"
              class="scene-item"
              :class="{ active: scene.id === selectedSceneId }"
              @click="selectScene(scene)"
            >
              <strong>场 {{ scene.sceneIndex + 1 }}</strong>
              <span>{{ scene.location || '未标地点' }}</span>
              <small>{{ scene.timeDesc || '时间未标注' }}</small>
            </button>
          </div>
        </aside>

        <section class="source-column">
          <div class="column-head">
            <NIcon :component="FileText" :size="15" />
            <span>原文</span>
          </div>
          <div class="source-body">
            <p v-if="selectedScene">{{ selectedScene.sourceContent || '无原文片段' }}</p>
            <NEmpty v-else description="请选择场景" />
          </div>
        </section>

        <section class="script-column">
          <div class="column-head">
            <NIcon :component="ScrollText" :size="15" />
            <span>剧本</span>
            <small>版本 {{ selectedScene?.version ?? '-' }}</small>
          </div>
          <NInput
            v-if="selectedScene"
            v-model:value="editContent"
            type="textarea"
            :bordered="false"
            class="script-editor"
            placeholder="在此编辑剧本内容..."
            @keydown.ctrl.s.prevent="saveScene"
            @keydown.meta.s.prevent="saveScene"
          />
          <NEmpty v-else description="请选择场景" class="pane-empty" />
        </section>

        <aside class="suggest-column">
          <div class="column-head">
            <NIcon :component="Layers" :size="15" />
            <span>改编建议</span>
          </div>
          <div class="suggest-body">
            <div class="suggest-card">
              <strong>心理外化</strong>
              <p>当前默认以动作外化处理内心活动。必要时可在剧本文本中改为对白或独白。</p>
            </div>
            <div class="suggest-card">
              <strong>格式检查</strong>
              <p>导出前请检查场景标题、人物名和对白换行。格式提醒不阻止导出。</p>
            </div>
            <div class="suggest-card">
              <strong>保存策略</strong>
              <p>保存时携带场景版本号，如其他设备已修改，会要求你明确处理冲突。</p>
            </div>
          </div>
        </aside>
      </main>
    </template>

    <NModal
      v-model:show="showConflict"
      preset="card"
      title="内容发生冲突"
      :closable="false"
      :mask-closable="false"
      style="width: min(720px, 96vw)"
    >
      <p class="conflict-desc">
        此场景已被其他设备或标签页修改。为避免覆盖，当前保存已暂停。
      </p>
      <div class="conflict-box">
        <strong>我的本地版本</strong>
        <pre>{{ conflictLocalText }}</pre>
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showConflict = false">继续本地编辑</NButton>
          <NButton type="primary" @click="discardLocalConflict">使用服务器版本</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.script-page {
  height: calc(100vh - var(--w-topbar-height));
  overflow: hidden;
  background: var(--w-bg-canvas);
}

.mobile-hint {
  display: none;
}

.script-loading {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.draft-entry {
  height: 100%;
  overflow-y: auto;
  padding: var(--w-space-6);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
  margin-bottom: var(--w-space-5);
}

.back-link:hover {
  color: var(--w-text);
}

.draft-entry-head {
  margin-bottom: var(--w-space-5);
}

.draft-entry-grid {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
  gap: var(--w-space-4);
}

.draft-start-card,
.draft-list-card {
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-lg);
  background: var(--w-bg-secondary);
  padding: var(--w-space-5);
}

.draft-start-card {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: var(--w-space-3);
  color: var(--w-text);
  text-align: left;
  transition: all var(--w-transition-base);
}

.draft-start-card:not(:disabled):hover {
  border-color: var(--w-brand);
  background: var(--w-brand-soft);
}

.draft-start-card span {
  color: var(--w-text-secondary);
  line-height: 1.7;
}

.draft-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--w-space-3);
}

.draft-list {
  display: flex;
  flex-direction: column;
  gap: var(--w-space-2);
}

.draft-item {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--w-border-subtle);
  border-radius: var(--w-radius-md);
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  text-align: left;
  color: var(--w-text);
}

.draft-item:hover {
  background: var(--w-bg-hover);
}

.draft-item span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.draft-item small {
  color: var(--w-text-tertiary);
}

.draft-arrow {
  margin-left: auto;
  transform: rotate(180deg);
  color: var(--w-text-tertiary);
}

.script-topbar {
  height: 58px;
  padding: 0 var(--w-space-4);
  border-bottom: 1px solid var(--w-border-subtle);
  background: var(--w-bg-toolbar);
  display: flex;
  align-items: center;
  gap: var(--w-space-4);
}

.script-title {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.script-title strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.script-title span {
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
}

.script-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: var(--w-space-2);
}

.export-select {
  width: 172px;
}

.script-workbench {
  height: calc(100% - 58px);
  display: grid;
  grid-template-columns: 220px minmax(220px, 0.85fr) minmax(360px, 1.4fr) 260px;
  overflow: hidden;
}

.scene-column,
.source-column,
.script-column,
.suggest-column {
  min-width: 0;
  border-right: 1px solid var(--w-border-subtle);
  background: var(--w-bg-secondary);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.script-column {
  background: var(--w-bg-paper);
}

.suggest-column {
  border-right: 0;
}

.column-head {
  height: 42px;
  padding: 0 var(--w-space-3);
  border-bottom: 1px solid var(--w-border-subtle);
  background: var(--w-bg-toolbar);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-xs);
  font-weight: 600;
}

.column-head small {
  margin-left: auto;
  color: var(--w-text-tertiary);
  font-weight: 400;
}

.episode-box {
  padding: var(--w-space-3);
  border-bottom: 1px solid var(--w-border-subtle);
}

.episode-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--w-space-2);
  font-size: var(--w-text-sm);
}

.episode-title small {
  color: var(--w-text-tertiary);
}

.episode-create {
  display: grid;
  grid-template-columns: 1fr 34px;
  gap: 6px;
  margin-bottom: var(--w-space-2);
}

.episode-chip {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 6px 8px;
  border-radius: var(--w-radius-sm);
  color: var(--w-text-secondary);
  font-size: var(--w-text-xs);
}

.episode-chip:hover {
  background: var(--w-bg-hover);
  color: var(--w-text);
}

.scene-list,
.source-body,
.suggest-body {
  flex: 1;
  overflow-y: auto;
}

.scene-item {
  width: 100%;
  padding: 11px var(--w-space-3);
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  color: var(--w-text-secondary);
  text-align: left;
}

.scene-item:hover {
  background: var(--w-bg-hover);
  color: var(--w-text);
}

.scene-item.active {
  background: var(--w-brand-soft);
  box-shadow: inset 2px 0 0 var(--w-brand);
  color: var(--w-text);
}

.scene-item span,
.scene-item small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scene-item small {
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
}

.source-body {
  padding: var(--w-space-4);
}

.source-body p {
  color: var(--w-text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
}

.script-editor {
  flex: 1;
  background: transparent !important;
}

.script-editor :deep(.n-input__textarea-el) {
  height: 100% !important;
  padding: var(--w-space-5);
  font-family: var(--w-font-mono);
  font-size: 14px;
  line-height: 1.85;
  color: var(--w-text) !important;
}

.pane-empty {
  margin: auto;
}

.suggest-body {
  padding: var(--w-space-3);
}

.suggest-card {
  padding: 12px;
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-tertiary);
  margin-bottom: var(--w-space-2);
}

.suggest-card strong {
  font-size: var(--w-text-sm);
}

.suggest-card p {
  margin-top: 6px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-xs);
  line-height: 1.7;
}

.conflict-desc {
  color: var(--w-text-secondary);
  margin-bottom: var(--w-space-3);
}

.conflict-box {
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  overflow: hidden;
}

.conflict-box strong {
  display: block;
  padding: 10px 12px;
  border-bottom: 1px solid var(--w-border-subtle);
}

.conflict-box pre {
  max-height: 280px;
  overflow: auto;
  padding: 12px;
  color: var(--w-text-secondary);
  white-space: pre-wrap;
  font-family: var(--w-font-mono);
  font-size: var(--w-text-sm);
}

@media (max-width: 768px) {
  .script-page {
    height: auto;
    min-height: calc(100vh - var(--w-topbar-height));
    overflow: auto;
    padding: var(--w-space-4);
  }

  .mobile-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: var(--w-space-3);
    border: 1px solid var(--w-warning-soft);
    border-radius: var(--w-radius-md);
    background: var(--w-warning-soft);
    color: var(--w-warning);
    font-size: var(--w-text-sm);
  }

  .draft-entry,
  .script-topbar,
  .script-workbench {
    display: none;
  }
}
</style>
