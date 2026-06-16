<script setup lang="ts">
/**
 * 编辑器左侧图标面板 + 滑出侧栏。
 *
 * 图标：大纲 / 角色库 / 世界观词典。
 * 点击图标切换对应面板，再次点击收起。
 */
import { ref, watch, computed } from 'vue'
import { NScrollbar, NInput, NEmpty, NSpin, NIcon, NTooltip } from 'naive-ui'
import {
  List,
  Users,
  BookMarked,
  Lock,
  Search,
  ChevronRight,
  ChevronDown,
} from 'lucide-vue-next'
import { listCharacters, listWorldElements } from '@/api/character'
import type { CharacterInfo, WorldElementInfo } from '@/api/character'
import type { OutlineInfo } from '@/api/project'

const props = defineProps<{
  projectId: string
  chapterId?: string
  outline: OutlineInfo | null
}>()

const emit = defineEmits<{
  selectChapter: [chapterId: string]
}>()

type PanelType = 'outline' | 'characters' | 'world'
const activePanel = ref<PanelType | null>('outline')

const characters = ref<CharacterInfo[]>([])
const charSearch = ref('')
const charLoading = ref(false)

const worldElements = ref<WorldElementInfo[]>([])
const worldSearch = ref('')
const worldLoading = ref(false)

const expandedVolumes = ref<Set<string>>(new Set())

const panelMeta = {
  outline: { label: '大纲', icon: List },
  characters: { label: '角色库', icon: Users },
  world: { label: '世界观', icon: BookMarked },
}

function togglePanel(panel: PanelType) {
  if (activePanel.value === panel) {
    activePanel.value = null
    return
  }
  activePanel.value = panel
  if (panel === 'characters' && !characters.value.length) loadCharacters()
  if (panel === 'world' && !worldElements.value.length) loadWorldElements()
}

function toggleVolume(volId: string) {
  if (expandedVolumes.value.has(volId)) {
    expandedVolumes.value.delete(volId)
  } else {
    expandedVolumes.value.add(volId)
  }
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

const filteredCharacters = computed(() =>
  characters.value.filter(c =>
    !charSearch.value || c.name?.toLowerCase().includes(charSearch.value.toLowerCase()))
)

const filteredElements = computed(() =>
  worldElements.value.filter(e =>
    !worldSearch.value || e.name?.toLowerCase().includes(worldSearch.value.toLowerCase()))
)

const groupedElements = computed(() => {
  const groups: Record<string, WorldElementInfo[]> = {}
  filteredElements.value.forEach(e => {
    const type = e.type || '其他'
    if (!groups[type]) groups[type] = []
    groups[type].push(e)
  })
  return groups
})

watch(() => props.outline, (newOutline) => {
  if (newOutline?.volumes) {
    newOutline.volumes.forEach(v => expandedVolumes.value.add(v.id))
  }
}, { immediate: true })

watch(() => props.projectId, () => {
  characters.value = []
  worldElements.value = []
})
</script>

<template>
  <div class="side-panel-root">
    <!-- 左侧图标条 -->
    <div class="icon-bar">
      <NTooltip v-for="(meta, key) in panelMeta" :key="key" placement="right">
        <template #trigger>
          <button
            :class="['icon-btn', activePanel === key && 'active']"
            @click="togglePanel(key as PanelType)"
          >
            <NIcon :component="meta.icon" :size="18" />
          </button>
        </template>
        {{ meta.label }}
      </NTooltip>
    </div>

    <!-- 滑出侧栏 -->
    <div v-if="activePanel" class="slide-panel">
      <!-- 大纲面板 -->
      <template v-if="activePanel === 'outline'">
        <div class="panel-header">
          <span class="panel-title">大纲</span>
        </div>
        <NScrollbar style="flex: 1">
          <template v-if="outline">
            <div v-for="vol in outline.volumes" :key="vol.id" class="volume-group">
              <button class="volume-title" @click="toggleVolume(vol.id)">
                <NIcon
                  :component="expandedVolumes.has(vol.id) ? ChevronDown : ChevronRight"
                  :size="14"
                  class="volume-arrow"
                />
                <span>{{ vol.title || '未命名卷' }}</span>
              </button>
              <div v-if="expandedVolumes.has(vol.id)" class="volume-chapters">
                <div
                  v-for="ch in vol.chapters"
                  :key="ch.id"
                  :class="['outline-item', ch.id === chapterId && 'active']"
                  @click="emit('selectChapter', ch.id)"
                >
                  <span class="outline-item-title">{{ ch.title || '未命名章节' }}</span>
                  <span class="outline-item-count">{{ ch.wordCount }}字</span>
                </div>
              </div>
            </div>
          </template>
          <NEmpty v-else description="暂无大纲" />
        </NScrollbar>
      </template>

      <!-- 角色库面板 -->
      <template v-else-if="activePanel === 'characters'">
        <div class="panel-header">
          <span class="panel-title">角色库</span>
        </div>
        <div class="panel-search">
          <NIcon :component="Search" :size="14" class="panel-search-icon" />
          <NInput v-model:value="charSearch" placeholder="搜索角色…" clearable size="small" />
        </div>
        <NScrollbar style="flex: 1">
          <NSpin :show="charLoading" size="small">
            <NEmpty v-if="!charLoading && !filteredCharacters.length" description="暂无角色" />
            <div v-for="c in filteredCharacters" :key="c.id" class="entity-card">
              <div class="entity-card-header">
                <span class="entity-name">{{ c.name }}</span>
                <div class="entity-tags">
                  <span v-if="c.role" class="entity-tag">{{ c.role }}</span>
                  <span v-if="c.locked" class="entity-tag entity-tag--locked">
                    <NIcon :component="Lock" :size="10" />
                    锁定
                  </span>
                </div>
              </div>
              <p class="entity-desc">{{ c.personality || '暂无描述' }}</p>
            </div>
          </NSpin>
        </NScrollbar>
      </template>

      <!-- 世界观词典面板 -->
      <template v-else>
        <div class="panel-header">
          <span class="panel-title">世界观词典</span>
        </div>
        <div class="panel-search">
          <NIcon :component="Search" :size="14" class="panel-search-icon" />
          <NInput v-model:value="worldSearch" placeholder="搜索词条…" clearable size="small" />
        </div>
        <NScrollbar style="flex: 1">
          <NSpin :show="worldLoading" size="small">
            <NEmpty v-if="!worldLoading && !filteredElements.length" description="暂无词条" />
            <div v-for="(items, type) in groupedElements" :key="type" class="entity-group">
              <div class="entity-group-title">{{ type }}</div>
              <div v-for="e in items" :key="e.id" class="entity-card">
                <div class="entity-card-header">
                  <span class="entity-name">{{ e.name }}</span>
                  <span v-if="e.locked" class="entity-tag entity-tag--locked">
                    <NIcon :component="Lock" :size="10" />
                    锁定
                  </span>
                </div>
                <p class="entity-desc">{{ e.description || '暂无描述' }}</p>
              </div>
            </div>
          </NSpin>
        </NScrollbar>
      </template>
    </div>
  </div>
</template>

<style scoped>
.side-panel-root {
  display: flex;
  height: 100%;
  flex-shrink: 0;
}

.icon-bar {
  width: 52px;
  background: var(--w-bg-secondary);
  border-right: 1px solid var(--w-border-subtle);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--w-space-3) 0;
  gap: var(--w-space-2);
  flex-shrink: 0;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--w-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--w-text-tertiary);
  transition: all var(--w-transition-fast);
}

.icon-btn:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.icon-btn.active {
  color: var(--w-text);
  background: var(--w-brand-soft);
  border-left: 2px solid var(--w-brand);
  margin-left: -1px;
}

.slide-panel {
  width: 260px;
  border-right: 1px solid var(--w-border-subtle);
  background: var(--w-bg-secondary);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.panel-header {
  height: 48px;
  padding: 0 var(--w-space-4);
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--w-border-subtle);
  flex-shrink: 0;
}

.panel-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
}

.panel-search {
  padding: var(--w-space-3) var(--w-space-4);
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--w-border-subtle);
}

.panel-search-icon {
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

.panel-search :deep(.n-input) {
  background: transparent !important;
}

/* 大纲 */
.volume-group {
  border-bottom: 1px solid var(--w-border-subtle);
}

.volume-title {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px var(--w-space-4);
  font-size: var(--w-text-xs);
  font-weight: 600;
  color: var(--w-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  transition: background var(--w-transition-fast);
}

.volume-title:hover {
  background: var(--w-bg-hover);
}

.volume-arrow {
  transition: transform var(--w-transition-fast);
}

.volume-chapters {
  padding-bottom: var(--w-space-2);
}

.outline-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--w-space-2);
  padding: 7px var(--w-space-4) 7px calc(var(--w-space-4) + 18px);
  cursor: pointer;
  transition: all var(--w-transition-fast);
}

.outline-item:hover {
  background: var(--w-bg-hover);
}

.outline-item.active {
  background: var(--w-brand-soft);
  border-left: 2px solid var(--w-brand);
  padding-left: calc(var(--w-space-4) + 16px);
}

.outline-item-title {
  font-size: var(--w-text-sm);
  color: var(--w-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outline-item-count {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

/* 实体卡片 */
.entity-group {
  padding: var(--w-space-3) 0;
  border-bottom: 1px solid var(--w-border-subtle);
}

.entity-group-title {
  padding: 0 var(--w-space-4);
  font-size: var(--w-text-xs);
  font-weight: 600;
  color: var(--w-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--w-space-2);
}

.entity-card {
  padding: 10px var(--w-space-4);
  border-bottom: 1px solid var(--w-border-subtle);
  transition: background var(--w-transition-fast);
}

.entity-card:last-child {
  border-bottom: none;
}

.entity-card:hover {
  background: var(--w-bg-hover);
}

.entity-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--w-space-2);
  margin-bottom: 4px;
}

.entity-name {
  font-size: var(--w-text-sm);
  font-weight: 500;
  color: var(--w-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entity-tags {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.entity-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: var(--w-radius-sm);
  background: var(--w-bg-tertiary);
  color: var(--w-text-secondary);
}

.entity-tag--locked {
  background: var(--w-brand-soft);
  color: var(--w-brand);
}

.entity-desc {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}
</style>
