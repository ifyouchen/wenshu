<script setup lang="ts">
/**
 * 剧本四栏工作台（P8-13）。
 * 布局：场景目录 | 原文 | 剧本（可编辑，乐观锁）| AI 建议（可收起）
 *
 * 约束：
 *  - 场景列表从后端分页加载（GET /script/drafts/{id}/scenes）。
 *  - 编辑场景内容使用乐观锁（version），版本冲突返回 409 时提示用户。
 *  - 移动端显示提示"复杂剧本工作台请使用 PC 访问"。
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NLayout, NLayoutSider, NLayoutContent, NScrollbar,
  NText, NTag, NSpin, NEmpty, NButton, NSpace, NInput,
  NAlert, useMessage, NPageHeader, NDivider,
} from 'naive-ui'
import { getDraft, listScenes, updateScene } from '@/api/script'
import type { ScriptDraftInfo, ScriptSceneInfo } from '@/api/types'

const route = useRoute()
const router = useRouter()
const message = useMessage()

/** 草稿 ID（路由参数）。 */
const draftId = computed(() => route.params.draftId as string | undefined)
/** 作品 ID（路由参数）。 */
const projectId = computed(() => route.params.projectId as string)

const draft = ref<ScriptDraftInfo | null>(null)
const scenes = ref<ScriptSceneInfo[]>([])
const totalScenes = ref(0)
const loadingDraft = ref(false)
const loadingScenes = ref(false)
const saving = ref(false)

/** 当前选中的场景。 */
const selectedScene = ref<ScriptSceneInfo | null>(null)
/** 当前编辑的脚本内容（绑定到编辑器）。 */
const editContent = ref('')
/** 是否显示 AI 建议面板。 */
const showAiPanel = ref(true)
/** AI 建议文本（占位，后续可接入分支建议 API）。 */
const aiSuggestions = ref<string[]>([])

/** 加载草稿基本信息。 */
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

/** 加载场景列表（分页）。 */
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

/** 选中场景，加载内容到编辑器。 */
function selectScene(scene: ScriptSceneInfo) {
  selectedScene.value = scene
  editContent.value = scene.content ?? ''
  // 占位 AI 建议
  aiSuggestions.value = [
    '可考虑增加人物对话细节…',
    '此场景情绪转变可更加自然…',
  ]
}

/**
 * 保存场景内容（含乐观锁校验）。
 * 版本冲突时显示错误提示，由用户决定如何处理。
 */
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
    // 更新本地版本号，避免重复保存时版本冲突
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
  <!-- 移动端提示 -->
  <div class="mobile-hint">
    <NAlert type="warning" title="请使用 PC 访问">
      剧本工作台为多栏布局，在手机或平板上体验较差，建议在 PC 端打开。
    </NAlert>
  </div>

  <!-- 四栏工作台主布局 -->
  <NLayout class="script-layout" has-sider>

    <!-- 第 1 栏：场景目录（180px）-->
    <NLayoutSider
      bordered
      :width="180"
      style="overflow: hidden; display: flex; flex-direction: column"
    >
      <div class="panel-header">
        <NText strong style="font-size: 13px">场景目录</NText>
        <NText depth="3" style="font-size: 11px">共 {{ totalScenes }} 场</NText>
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
              :depth="selectedScene?.id === scene.id ? 1 : 3"
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
    <NLayoutContent style="display: flex; flex-direction: column; overflow: hidden">

      <!-- 顶部工具栏 -->
      <div class="toolbar">
        <NPageHeader
          :title="draft?.title ?? '剧本工作台'"
          :subtitle="selectedScene ? `#${selectedScene.sceneIndex + 1} ${selectedScene.location ?? ''}` : '请选择场景'"
          style="padding: 0"
          @back="router.push(`/projects/${projectId}/editor`)"
        />
        <NSpace>
          <NButton size="small" :loading="saving" type="primary" @click="saveScene"
                   :disabled="!selectedScene">保存场景</NButton>
          <NButton size="small" text @click="showAiPanel = !showAiPanel">
            {{ showAiPanel ? '收起 AI' : '展开 AI' }}
          </NButton>
        </NSpace>
      </div>
      <NDivider style="margin: 0" />

      <!-- 双栏内容区：原文 + 剧本 -->
      <div class="content-area">

        <!-- 第 2 栏：原文（只读）-->
        <div class="col-pane">
          <div class="col-header">
            <NTag size="small" type="default" :bordered="false">原文</NTag>
          </div>
          <NScrollbar class="col-body">
            <div v-if="selectedScene" class="source-text">
              {{ selectedScene.sourceContent || '（无原文）' }}
            </div>
            <NEmpty v-else description="请选择场景" style="padding: 32px 0" />
          </NScrollbar>
        </div>

        <!-- 第 3 栏：剧本（可编辑）-->
        <div class="col-pane">
          <div class="col-header">
            <NTag size="small" type="success" :bordered="false">剧本</NTag>
            <NText depth="3" style="font-size: 11px">
              版本 {{ selectedScene?.version ?? '-' }}
            </NText>
          </div>
          <div v-if="selectedScene" style="height: 100%; display: flex; flex-direction: column">
            <NInput
              v-model:value="editContent"
              type="textarea"
              style="flex: 1; border: none; border-radius: 0"
              :bordered="false"
              placeholder="在此编辑剧本内容…"
              @keydown.ctrl.s.prevent="saveScene"
              @keydown.meta.s.prevent="saveScene"
            />
          </div>
          <NEmpty v-else description="请选择场景" style="padding: 32px 0" />
        </div>

        <!-- 第 4 栏：AI 建议（可收起）-->
        <div v-if="showAiPanel" class="col-pane ai-col">
          <div class="col-header">
            <NTag size="small" type="info" :bordered="false">AI 建议</NTag>
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
</template>

<style scoped>
/* 移动端提示：仅在小屏幕显示，PC 隐藏 */
.mobile-hint { display: none; padding: 16px; }
@media (max-width: 768px) {
  .mobile-hint { display: block; }
  .script-layout { display: none !important; }
}

.script-layout { height: 100vh; }

.panel-header {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.scene-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #fafafa;
  display: flex;
  flex-direction: column;
  transition: background 0.1s;
}
.scene-item:hover { background: rgba(0,0,0,0.04); }
.scene-item.active { background: rgba(99,125,245,0.1); }

.toolbar {
  padding: 8px 16px;
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
  border-right: 1px solid #f0f0f0;
  overflow: hidden;
}
.col-pane:last-child { border-right: none; }

.ai-col { max-width: 220px; flex: 0 0 220px; }

.col-header {
  padding: 6px 12px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  background: #fafafa;
}

.col-body { flex: 1; }

.source-text {
  padding: 16px;
  font-size: 14px;
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
}

.ai-suggestion {
  padding: 10px 12px;
  border: 1px solid #e8f4ff;
  border-radius: 6px;
  background: #f0f7ff;
  margin-bottom: 8px;
}
</style>
