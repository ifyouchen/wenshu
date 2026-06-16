<script setup lang="ts">
/**
 * 剧本四栏工作台。
 *
 * 布局：场景目录 | 原文 | 剧本（可编辑，乐观锁）| AI 建议（可收起）
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NLayout, NLayoutSider, NLayoutContent, NScrollbar,
  NText, NSpin, NEmpty, NButton, NSpace, NInput,
  NAlert, useMessage, NPageHeader, NIcon,
} from 'naive-ui'
import {
  FileText,
  ScrollText,
  Lightbulb,
  Save,
  PanelRight,
  AlertTriangle,
  ChevronRight,
} from 'lucide-vue-next'
import { getDraft, listScenes, updateScene } from '@/api/script'
import type { ScriptDraftInfo, ScriptSceneInfo } from '@/api/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const draftId = computed(() => route.params.draftId as string | undefined)
const projectId = computed(() => route.params.projectId as string)

const draft = ref<ScriptDraftInfo | null>(null)
const scenes = ref<ScriptSceneInfo[]>([])
const totalScenes = ref(0)
const loadingDraft = ref(false)
const loadingScenes = ref(false)
const saving = ref(false)

const selectedScene = ref<ScriptSceneInfo | null>(null)
const editContent = ref('')
const showAiPanel = ref(true)
const aiSuggestions = ref<string[]>([])

async function loadDraft() {
  if (!draftId.value) return
  loadingDraft.value = true
  try {
    const res = await getDraft(draftId.value)
    draft.value = res.data.data
  } catch {
    message.error('草稿加载失败')
  } finally {
    loadingDraft.value = false
  }
}

async function loadScenes(page = 0) {
  if (!draftId.value) return
  loadingScenes.value = true
  try {
    const res = await listScenes(draftId.value, page, 50)
    const { total, scenes: newScenes } = res.data.data
    totalScenes.value = total
    scenes.value = page === 0 ? newScenes : [...scenes.value, ...newScenes]
  } catch {
    message.error('场景列表加载失败')
  } finally {
    loadingScenes.value = false
  }
}

function selectScene(scene: ScriptSceneInfo) {
  selectedScene.value = scene
  editContent.value = scene.content ?? ''
  aiSuggestions.value = [
    '可考虑增加人物对话细节，让冲突更直接。',
    '此场景情绪转变可更加自然，建议增加过渡动作。',
  ]
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
    selectedScene.value.version = updated.version
    const idx = scenes.value.findIndex(s => s.id === updated.id)
    if (idx !== -1) scenes.value[idx] = { ...scenes.value[idx], ...updated }
    message.success('保存成功')
  } catch (err: unknown) {
    const status = (err as { response?: { status?: number } })?.response?.status
    if (status === 409) {
      message.error('版本冲突：该场景已被其他设备修改，请刷新后重试')
    } else {
      message.error('保存失败，请重试')
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadDraft()
  await loadScenes()
  if (scenes.value.length) selectScene(scenes.value[0])
})

watch(draftId, async (newId) => {
  if (newId) {
    await loadDraft()
    await loadScenes()
    if (scenes.value.length) selectScene(scenes.value[0])
  }
})
</script>

<template>
  <div class="script-page">
    <!-- 移动端提示 -->
    <div class="mobile-hint">
      <NAlert type="warning" title="请使用 PC 访问">
        <template #icon>
          <NIcon :component="AlertTriangle" :size="18" />
        </template>
        剧本工作台为多栏布局，在手机或平板上体验较差，建议在 PC 端打开。
      </NAlert>
    </div>

    <!-- 四栏工作台主布局 -->
    <NLayout class="script-layout" has-sider>
      <!-- 第 1 栏：场景目录 -->
      <NLayoutSider
        bordered
        :width="180"
        class="scene-sider"
      >
        <div class="panel-header">
          <NIcon :component="ScrollText" :size="16" class="panel-header-icon" />
          <span class="panel-header-title">场景目录</span>
          <span class="panel-header-count">共 {{ totalScenes }} 场</span>
        </div>
        <NScrollbar style="flex: 1">
          <NSpin :show="loadingScenes" size="small">
            <div
              v-for="scene in scenes"
              :key="scene.id"
              :class="['scene-item', selectedScene?.id === scene.id && 'active']"
              @click="selectScene(scene)"
            >
              <NText
                :depth="selectedScene?.id === scene.id ? 1 : 2"
                style="font-size: 12px; font-weight: 500"
              >
                #{{ scene.sceneIndex + 1 }}
                {{ scene.location ? ` · ${scene.location}` : '' }}
              </NText>
              <NText depth="3" style="font-size: 11px; margin-top: 2px">
                {{ scene.timeDesc || '' }}
              </NText>
            </div>
          </NSpin>
        </NScrollbar>
      </NLayoutSider>

      <!-- 中间主区域 -->
      <NLayoutContent class="script-content">
        <!-- 顶部工具栏 -->
        <div class="script-toolbar">
          <NPageHeader
            :title="draft?.title ?? '剧本工作台'"
            :subtitle="selectedScene ? `#${selectedScene.sceneIndex + 1} ${selectedScene.location ?? ''}` : '请选择场景'"
            style="padding: 0"
            @back="router.push(`/projects/${projectId}/editor`)"
          >
            <template #back>
              <NIcon :component="ChevronRight" :size="18" style="transform: rotate(180deg)" />
            </template>
          </NPageHeader>
          <NSpace>
            <NButton
              size="small"
              type="primary"
              :loading="saving"
              :disabled="!selectedScene"
              @click="saveScene"
            >
              <template #icon>
                <NIcon :component="Save" :size="14" />
              </template>
              保存场景
            </NButton>
            <NButton size="small" ghost @click="showAiPanel = !showAiPanel">
              <template #icon>
                <NIcon :component="PanelRight" :size="14" />
              </template>
              {{ showAiPanel ? '收起 AI' : '展开 AI' }}
            </NButton>
          </NSpace>
        </div>

        <!-- 双栏内容区：原文 + 剧本 -->
        <div class="content-area">
          <!-- 第 2 栏：原文 -->
          <div class="col-pane">
            <div class="col-header">
              <NIcon :component="FileText" :size="14" />
              <span>原文</span>
            </div>
            <NScrollbar class="col-body">
              <div v-if="selectedScene" class="source-text">
                {{ selectedScene.sourceContent || '（无原文）' }}
              </div>
              <NEmpty v-else description="请选择场景" style="padding: 32px 0" />
            </NScrollbar>
          </div>

          <!-- 第 3 栏：剧本 -->
          <div class="col-pane col-pane--main">
            <div class="col-header">
              <NIcon :component="ScrollText" :size="14" />
              <span>剧本</span>
              <span class="col-header-meta">版本 {{ selectedScene?.version ?? '-' }}</span>
            </div>
            <div v-if="selectedScene" class="script-editor">
              <NInput
                v-model:value="editContent"
                type="textarea"
                class="script-textarea"
                :bordered="false"
                placeholder="在此编辑剧本内容…"
                @keydown.ctrl.s.prevent="saveScene"
                @keydown.meta.s.prevent="saveScene"
              />
            </div>
            <NEmpty v-else description="请选择场景" style="padding: 32px 0" />
          </div>

          <!-- 第 4 栏：AI 建议 -->
          <div v-if="showAiPanel" class="col-pane ai-col">
            <div class="col-header">
              <NIcon :component="Lightbulb" :size="14" />
              <span>AI 建议</span>
            </div>
            <NScrollbar class="col-body">
              <div v-if="aiSuggestions.length" style="padding: 12px">
                <div
                  v-for="(sug, idx) in aiSuggestions"
                  :key="idx"
                  class="ai-suggestion"
                >
                  <NText depth="2" style="font-size: 13px">{{ sug }}</NText>
                </div>
              </div>
              <NEmpty v-else description="暂无 AI 建议" style="padding: 24px 0" />
            </NScrollbar>
          </div>
        </div>
      </NLayoutContent>
    </NLayout>
  </div>
</template>

<style scoped>
.script-page {
  height: calc(100vh - var(--w-topbar-height));
  overflow: hidden;
}

.mobile-hint {
  display: none;
  padding: 16px;
}

@media (max-width: 768px) {
  .mobile-hint {
    display: block;
  }
  .script-layout {
    display: none !important;
  }
  .script-page {
    height: auto;
    overflow: auto;
  }
}

.script-layout {
  height: 100%;
}

.scene-sider {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--w-bg-secondary) !important;
  border-right: 1px solid var(--w-border-subtle) !important;
}

.panel-header {
  height: 48px;
  padding: 0 var(--w-space-3);
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.panel-header-icon {
  color: var(--w-brand);
}

.panel-header-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
}

.panel-header-count {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  margin-left: auto;
}

.scene-item {
  padding: 10px var(--w-space-3);
  cursor: pointer;
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  flex-direction: column;
  transition: background var(--w-transition-fast);
}

.scene-item:hover {
  background: var(--w-bg-hover);
}

.scene-item.active {
  background: var(--w-brand-soft);
  border-left: 2px solid var(--w-brand);
  padding-left: 10px;
}

.script-content {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--w-bg);
}

.script-toolbar {
  height: 52px;
  padding: 0 var(--w-space-4);
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.content-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.col-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--w-border-subtle);
  overflow: hidden;
}

.col-pane:last-child {
  border-right: none;
}

.col-pane--main {
  flex: 1.5;
}

.ai-col {
  max-width: 240px;
  flex: 0 0 240px;
}

.col-header {
  height: 40px;
  padding: 0 var(--w-space-3);
  border-bottom: 1px solid var(--w-border-subtle);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  background: var(--w-bg-secondary);
  font-size: var(--w-text-xs);
  font-weight: 600;
  color: var(--w-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.col-header-meta {
  margin-left: auto;
  font-weight: 400;
  text-transform: none;
  letter-spacing: 0;
}

.col-body {
  flex: 1;
}

.source-text {
  padding: var(--w-space-4);
  font-size: 14px;
  line-height: 1.8;
  color: var(--w-text-secondary);
  white-space: pre-wrap;
}

.script-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.script-textarea {
  flex: 1;
  background: transparent !important;
}

.script-textarea :deep(.n-input__textarea-el) {
  height: 100% !important;
  font-family: var(--w-font-mono);
  font-size: 14px;
  line-height: 1.8;
  color: var(--w-text);
  padding: var(--w-space-4);
}

.ai-suggestion {
  padding: 10px 12px;
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-tertiary);
  margin-bottom: 8px;
}
</style>
