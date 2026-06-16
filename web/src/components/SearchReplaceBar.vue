<script setup lang="ts">
/**
 * 全书搜索替换横条（P8-09）。
 * - 300ms debounce 搜索（后端执行，不在前端加载全卷）。
 * - Esc 关闭。
 * - 支持大小写敏感、整词匹配、同步角色档案。
 * - 替换前自动创建快照（由后端保证）。
 */
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import {
  NInput, NButton, NSpace, NCheckbox, NText, NCollapse, NCollapseItem,
  NSpin, useMessage,
} from 'naive-ui'
import { searchProject, replaceProject } from '@/api/search'
import type { SearchResult } from '@/api/search'

const props = defineProps<{
  projectId: string
  /** 是否显示替换区域。 */
  showReplace?: boolean
}>()

const emit = defineEmits<{
  /** 用户关闭搜索栏。 */
  close: []
  /** 用户点击跳转到某个章节。 */
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

/** 300ms debounce 搜索。 */
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
    // 刷新搜索结果
    await doSearch()
  } catch {
    message.error('替换失败')
  } finally {
    replaceLoading.value = false
  }
}

/** Esc 关闭搜索栏。 */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  // 自动聚焦搜索框
  nextTick(() => (searchInput.value as HTMLInputElement | null)?.focus?.())
})

onUnmounted(() => document.removeEventListener('keydown', handleKeydown))
</script>

<template>
  <div class="search-bar">
    <!-- 搜索行 -->
    <NSpace align="center" :size="8" :wrap="false">
      <NInput
        ref="searchInput"
        v-model:value="keyword"
        placeholder="全书搜索…（Esc 关闭）"
        size="small"
        style="width: 240px"
        clearable
        @keydown.enter="doSearch"
      >
        <template #suffix>
          <NSpin v-if="loading" size="small" />
          <span v-else-if="result" style="font-size: 11px; color: #999">{{ result.total }} 处</span>
        </template>
      </NInput>

      <NCheckbox v-model:checked="caseSensitive" size="small">Aa</NCheckbox>
      <NCheckbox v-model:checked="wholeWord" size="small">整词</NCheckbox>
      <NButton size="small" @click="doSearch" :loading="loading">搜索</NButton>
      <NButton text size="small" @click="emit('close')">✕</NButton>
    </NSpace>

    <!-- 替换行 -->
    <NSpace v-if="showReplace" align="center" :size="8" style="margin-top: 6px" :wrap="false">
      <NInput
        v-model:value="replacement"
        placeholder="替换为…"
        size="small"
        style="width: 240px"
      />
      <NCheckbox v-model:checked="syncCharacterName" size="small">同步角色档案</NCheckbox>
      <NButton size="small" type="warning" :loading="replaceLoading" @click="doReplace">
        全部替换
      </NButton>
    </NSpace>

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
            <NText depth="3" style="font-size: 12px">…{{ m.before }}</NText>
            <NText type="error" strong style="font-size: 12px; background: #fff3cd; padding: 0 2px">{{ m.match }}</NText>
            <NText depth="3" style="font-size: 12px">{{ m.after }}…</NText>
          </div>
          <NText v-if="ch.matches.length > 5" depth="3" style="font-size: 11px">
            还有 {{ ch.matchCount - 5 }} 处…
          </NText>
        </NCollapseItem>
      </NCollapse>
    </div>

    <NText
      v-else-if="result && !result.chapters.length"
      depth="3"
      style="font-size: 12px; margin-top: 6px; display: block"
    >
      未找到匹配内容
    </NText>
  </div>
</template>

<style scoped>
.search-bar {
  padding: 10px 16px;
  border-bottom: 2px solid #e8e8e8;
  background: #fafafa;
  position: relative;
  z-index: 50;
}
.search-results {
  max-height: 240px;
  overflow-y: auto;
  margin-top: 8px;
}
.match-item {
  padding: 3px 6px;
  cursor: pointer;
  border-radius: 4px;
  display: inline;
}
.match-item:hover { background: rgba(0,0,0,0.05); }
</style>
