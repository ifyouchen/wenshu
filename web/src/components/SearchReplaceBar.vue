<script setup lang="ts">
/**
 * 全书搜索替换横条。
 *
 * - 300ms debounce 搜索（后端执行）
 * - Esc 关闭
 * - 支持大小写敏感、整词匹配、同步角色档案
 * - 替换前自动创建快照（由后端保证）
 */
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { NButton, NCheckbox, NCollapse, NCollapseItem, NIcon, NSpin, useMessage } from 'naive-ui'
import { Search, Replace, X, CaseSensitive, WholeWord } from 'lucide-vue-next'
import { searchProject, replaceProject } from '@/api/search'
import type { SearchResult } from '@/api/search'

const props = defineProps<{
  projectId: string
  showReplace?: boolean
}>()

const emit = defineEmits<{
  close: []
  jumpToChapter: [chapterId: string]
}>()

const message = useMessage()

const keyword = ref('')
const replacement = ref('')
const caseSensitive = ref(false)
const wholeWord = ref(false)
const syncCharacterName = ref(false)
const loading = ref(false)
const replaceLoading = ref(false)
const result = ref<SearchResult | null>(null)
const searchInput = ref<HTMLInputElement | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch([keyword, caseSensitive, wholeWord], () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!keyword.value.trim()) { result.value = null; return }
  debounceTimer = setTimeout(doSearch, 300)
})

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const res = await searchProject(props.projectId, keyword.value, caseSensitive.value, wholeWord.value)
    result.value = res.data.data
  } catch {
    message.error('搜索失败')
  } finally {
    loading.value = false
  }
}

async function doReplace() {
  if (!keyword.value.trim()) return
  replaceLoading.value = true
  try {
    const res = await replaceProject(props.projectId, {
      keyword: keyword.value,
      replacement: replacement.value,
      caseSensitive: caseSensitive.value,
      wholeWord: wholeWord.value,
      syncCharacterName: syncCharacterName.value,
    })
    const r = res.data.data
    message.success(
      `已替换 ${r.totalReplaced} 处` +
      (r.characterNameSynced ? '，角色档案已同步' : '') +
      '（替换前快照已创建）',
    )
    await doSearch()
  } catch {
    message.error('替换失败')
  } finally {
    replaceLoading.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  nextTick(() => (searchInput.value as HTMLInputElement | null)?.focus?.())
})

onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div class="search-bar">
    <div class="search-bar-inner">
      <!-- 搜索行 -->
      <div class="search-row">
        <div class="search-input-wrap">
          <NIcon :component="Search" :size="16" class="search-input-icon" />
          <input
            ref="searchInput"
            v-model="keyword"
            class="search-input"
            placeholder="全书搜索…（Esc 关闭）"
            @keydown.enter="doSearch"
          />
          <NSpin v-if="loading" size="small" class="search-loading" />
          <span v-else-if="result" class="search-count">{{ result.total }} 处</span>
        </div>

        <div class="search-options">
          <label class="search-option" :class="{ active: caseSensitive }">
            <NIcon :component="CaseSensitive" :size="14" />
            <NCheckbox v-model:checked="caseSensitive" size="small">区分大小写</NCheckbox>
          </label>
          <label class="search-option" :class="{ active: wholeWord }">
            <NIcon :component="WholeWord" :size="14" />
            <NCheckbox v-model:checked="wholeWord" size="small">全词匹配</NCheckbox>
          </label>
          <NButton size="small" @click="doSearch" :loading="loading">
            搜索
          </NButton>
          <NButton text class="search-close" @click="emit('close')">
            <NIcon :component="X" :size="18" />
          </NButton>
        </div>
      </div>

      <!-- 替换行 -->
      <div v-if="showReplace" class="replace-row">
        <div class="search-input-wrap">
          <NIcon :component="Replace" :size="16" class="search-input-icon" />
          <input
            v-model="replacement"
            class="search-input"
            placeholder="替换为…"
          />
        </div>

        <div class="replace-options">
          <label class="search-option" :class="{ active: syncCharacterName }">
            <NCheckbox v-model:checked="syncCharacterName" size="small">同步角色档案</NCheckbox>
          </label>
          <NButton size="small" type="primary" :loading="replaceLoading" @click="doReplace">
            全部替换
          </NButton>
        </div>
      </div>

      <!-- 搜索结果 -->
      <div v-if="result && result.chapters.length" class="search-results">
        <NCollapse :default-expanded-names="result.chapters.slice(0, 3).map(c => c.chapterId)">
          <NCollapseItem
            v-for="ch in result.chapters"
            :key="ch.chapterId"
            :title="`${ch.chapterTitle || '未命名章节'}（${ch.matchCount} 处）`"
            :name="ch.chapterId"
          >
            <div
              v-for="(m, idx) in ch.matches.slice(0, 5)"
              :key="idx"
              class="match-item"
              @click="emit('jumpToChapter', ch.chapterId)"
            >
              <span class="match-context">{{ m.before }}</span>
              <span class="match-highlight">{{ m.match }}</span>
              <span class="match-context">{{ m.after }}</span>
            </div>
            <div v-if="ch.matches.length > 5" class="match-more">
              还有 {{ ch.matchCount - 5 }} 处…
            </div>
          </NCollapseItem>
        </NCollapse>
      </div>

      <div
        v-else-if="result && !result.chapters.length"
        class="search-empty"
      >
        未找到匹配内容
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-bar {
  background: var(--w-bg-secondary);
  border-bottom: 1px solid var(--w-border-subtle);
  position: relative;
  z-index: 50;
  flex-shrink: 0;
}

.search-bar-inner {
  max-width: var(--w-editor-max-width);
  margin: 0 auto;
  padding: 12px var(--w-space-4);
}

.search-row,
.replace-row {
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  flex-wrap: wrap;
}

.replace-row {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--w-border-subtle);
}

.search-input-wrap {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--w-bg-tertiary);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-sm);
  padding: 0 12px;
  transition: all var(--w-transition-base);
}

.search-input-wrap:focus-within {
  border-color: var(--w-brand);
  box-shadow: 0 0 0 3px var(--w-brand-soft);
}

.search-input-icon {
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 36px;
  background: transparent;
  border: none;
  outline: none;
  color: var(--w-text);
  font-size: var(--w-text-base);
}

.search-input::placeholder {
  color: var(--w-text-tertiary);
}

.search-loading,
.search-count {
  flex-shrink: 0;
}

.search-count {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

.search-options,
.replace-options {
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  flex-shrink: 0;
}

.search-option {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--w-text-xs);
  color: var(--w-text-secondary);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--w-radius-sm);
  transition: all var(--w-transition-fast);
}

.search-option:hover,
.search-option.active {
  background: var(--w-bg-hover);
  color: var(--w-text);
}

.search-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--w-text-tertiary);
  border-radius: var(--w-radius-sm);
}

.search-close:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.search-results {
  max-height: 260px;
  overflow-y: auto;
  margin-top: 12px;
  padding: 12px;
  background: var(--w-bg-tertiary);
  border-radius: var(--w-radius-md);
}

.match-item {
  padding: 6px 8px;
  cursor: pointer;
  border-radius: var(--w-radius-sm);
  font-size: var(--w-text-sm);
  line-height: 1.6;
  transition: background var(--w-transition-fast);
}

.match-item:hover {
  background: var(--w-bg-hover);
}

.match-context {
  color: var(--w-text-secondary);
}

.match-highlight {
  color: var(--w-brand);
  font-weight: 600;
  background: var(--w-brand-soft);
  padding: 0 3px;
  border-radius: 3px;
}

.match-more {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  padding: 4px 8px;
}

.search-empty {
  margin-top: 12px;
  font-size: var(--w-text-sm);
  color: var(--w-text-tertiary);
}

@media (max-width: 767px) {
  .search-row,
  .replace-row {
    flex-direction: column;
    align-items: stretch;
  }

  .search-options,
  .replace-options {
    flex-wrap: wrap;
  }
}
</style>
