<script setup lang="ts">
/**
 * 编辑器左侧图标面板 + 滑出侧栏（P8-07）。
 * 图标：大纲 / 角色库 / 世界观词典。
 * 点击图标切换对应面板，再次点击收起。
 */
import { ref, watch } from 'vue'
import { NScrollbar, NInput, NEmpty, NSpin, NTag, NText, NTooltip } from 'naive-ui'
import { listCharacters, listWorldElements } from '@/api/character'
import type { CharacterInfo, WorldElementInfo } from '@/api/character'
import type { OutlineInfo } from '@/api/project'

const props = defineProps<{
  projectId: string
  chapterId?: string
  outline: OutlineInfo | null
}>()

const emit = defineEmits<{
  /** 章节选中时通知父组件切换章节。 */
  selectChapter: [chapterId: string]
}>()


type PanelType = 'outline' | 'characters' | 'world'
const activePanel = ref<PanelType | null>(null)

// --- 角色库 ---
const characters = ref<CharacterInfo[]>([])
const charSearch = ref('')
const charLoading = ref(false)

// --- 世界观词典 ---
const worldElements = ref<WorldElementInfo[]>([])
const worldSearch = ref('')
const worldLoading = ref(false)

/** 切换面板（同图标再次点击则收起）。 */
function togglePanel(panel: PanelType) {
  if (activePanel.value === panel) {
    activePanel.value = null
    return
  }
  activePanel.value = panel
  if (panel === 'characters' && !characters.value.length) loadCharacters()
  if (panel === 'world' && !worldElements.value.length) loadWorldElements()
}

async function loadCharacters() {
  charLoading.value = true
  try {
    const res = await listCharacters(props.projectId)
    characters.value = res.data.data
  } finally {
    charLoading.value = false
  }
}

async function loadWorldElements() {
  worldLoading.value = true
  try {
    const res = await listWorldElements(props.projectId)
    worldElements.value = res.data.data
  } finally {
    worldLoading.value = false
  }
}

const filteredCharacters = () =>
  characters.value.filter(c =>
    !charSearch.value || c.name?.toLowerCase().includes(charSearch.value.toLowerCase()))

const filteredElements = () =>
  worldElements.value.filter(e =>
    !worldSearch.value || e.name?.toLowerCase().includes(worldSearch.value.toLowerCase()))

/** 重置当数据时重新加载。 */
watch(() => props.projectId, () => {
  characters.value = []
  worldElements.value = []
})
</script>

<template>
  <div class="side-panel-root">
    <!-- 左侧图标条 -->
    <div class="icon-bar">
      <NTooltip placement="right">
        <template #trigger>
          <div :class="['icon-btn', activePanel === 'outline' && 'active']"
               @click="togglePanel('outline')">📋</div>
        </template>大纲
      </NTooltip>
      <NTooltip placement="right">
        <template #trigger>
          <div :class="['icon-btn', activePanel === 'characters' && 'active']"
               @click="togglePanel('characters')">👤</div>
        </template>角色库
      </NTooltip>
      <NTooltip placement="right">
        <template #trigger>
          <div :class="['icon-btn', activePanel === 'world' && 'active']"
               @click="togglePanel('world')">🌍</div>
        </template>世界观
      </NTooltip>
    </div>

    <!-- 滑出侧栏 -->
    <div v-if="activePanel" class="slide-panel">
      <!-- 大纲面板 -->
      <template v-if="activePanel === 'outline'">
        <div class="panel-title">大纲</div>
        <NScrollbar style="flex: 1">
          <template v-if="outline">
            <div v-for="vol in outline.volumes" :key="vol.id">
              <NText strong style="font-size: 12px; padding: 6px 12px; display: block; color: #888">
                {{ vol.title || '未命名卷' }}
              </NText>
              <div
                v-for="ch in vol.chapters"
                :key="ch.id"
                :class="['outline-item', ch.id === chapterId && 'active']"
                @click="emit('selectChapter', ch.id)"
              >
                <NText :depth="ch.id === chapterId ? 1 : 3" style="font-size: 13px">
                  {{ ch.title || '未命名章节' }}
                </NText>
                <NText depth="3" style="font-size: 11px; white-space: nowrap">
                  {{ ch.wordCount }}字
                </NText>
              </div>
            </div>
          </template>
          <NEmpty v-else description="暂无大纲" />
        </NScrollbar>
      </template>

      <!-- 角色库面板 -->
      <template v-else-if="activePanel === 'characters'">
        <div class="panel-title">角色库</div>
        <NInput v-model:value="charSearch" placeholder="搜索角色…" clearable size="small"
                style="margin: 0 8px 8px" />
        <NScrollbar style="flex: 1">
          <NSpin :show="charLoading" size="small">
            <NEmpty v-if="!charLoading && !filteredCharacters().length" description="暂无角色" />
            <div v-for="c in filteredCharacters()" :key="c.id" class="char-item">
              <div style="display: flex; align-items: center; gap: 6px">
                <NText strong style="font-size: 13px">{{ c.name }}</NText>
                <NTag v-if="c.role" size="tiny" :bordered="false" type="info">{{ c.role }}</NTag>
                <NTag v-if="c.locked" size="tiny" :bordered="false" type="warning">🔒</NTag>
              </div>
              <NText depth="3" style="font-size: 12px; margin-top: 2px">
                {{ c.personality || '暂无描述' }}
              </NText>
            </div>
          </NSpin>
        </NScrollbar>
      </template>

      <!-- 世界观词典面板 -->
      <template v-else>
        <div class="panel-title">世界观词典</div>
        <NInput v-model:value="worldSearch" placeholder="搜索词条…" clearable size="small"
                style="margin: 0 8px 8px" />
        <NScrollbar style="flex: 1">
          <NSpin :show="worldLoading" size="small">
            <NEmpty v-if="!worldLoading && !filteredElements().length" description="暂无词条" />
            <div v-for="e in filteredElements()" :key="e.id" class="world-item">
              <div style="display: flex; align-items: center; gap: 6px">
                <NText strong style="font-size: 13px">{{ e.name }}</NText>
                <NTag v-if="e.type" size="tiny" :bordered="false">{{ e.type }}</NTag>
              </div>
              <NText depth="3" style="font-size: 12px; margin-top: 2px">
                {{ e.description || '暂无描述' }}
              </NText>
            </div>
          </NSpin>
        </NScrollbar>
      </template>
    </div>
  </div>
</template>

<style scoped>
.side-panel-root { display: flex; height: 100%; }

.icon-bar {
  width: 48px;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  gap: 8px;
  flex-shrink: 0;
}
.icon-btn {
  width: 36px; height: 36px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 18px;
  transition: background 0.15s;
}
.icon-btn:hover { background: rgba(0,0,0,0.06); }
.icon-btn.active { background: rgba(99,125,245,0.15); }

.slide-panel {
  width: 220px;
  border-right: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}
.panel-title {
  padding: 10px 12px;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.outline-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 5px 12px; cursor: pointer; gap: 8px;
}
.outline-item:hover { background: rgba(0,0,0,0.04); }
.outline-item.active { background: rgba(99,125,245,0.1); }

.char-item, .world-item {
  padding: 8px 12px;
  border-bottom: 1px solid #f7f7f7;
}
.char-item:hover, .world-item:hover { background: rgba(0,0,0,0.03); }
</style>
