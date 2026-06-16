<script setup lang="ts">
/**
 * 章节编辑器页面（P8-06）。
 * - 只加载当前章节（通过路由参数 chapterId）。
 * - 章节内容通过 getChapter API 单独加载，不加载全卷。
 * - 内容变更通过 ChapterEditor 的 change 事件触发自动保存。
 */
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NLayout, NLayoutSider, NLayoutContent, NSpin, NEmpty, NInput, NText, useMessage } from 'naive-ui'
import ChapterEditor from '@/components/ChapterEditor.vue'
import { getChapter, saveChapter, getOutline } from '@/api/project'
import type { ChapterInfo, OutlineInfo } from '@/api/project'

const route = useRoute()
const message = useMessage()

const chapter = ref<ChapterInfo | null>(null)
const outline = ref<OutlineInfo | null>(null)
const loading = ref(false)
const chapterTitle = ref('')
const editorRef = ref<InstanceType<typeof ChapterEditor> | null>(null)

const projectId = route.params.projectId as string
const chapterId = route.params.chapterId as string | undefined

/** 加载当前章节内容（只加载单章，不加载全卷）。 */
async function loadChapter(id: string) {
  loading.value = true
  try {
    const res = await getChapter(id)
    chapter.value = res.data.data
    chapterTitle.value = chapter.value.title ?? ''
  } catch {
    message.error('章节加载失败')
  } finally {
    loading.value = false
  }
}

/** 加载大纲（用于侧边导航）。 */
async function loadOutline() {
  try {
    const res = await getOutline(projectId)
    outline.value = res.data.data
  } catch {
    // 静默失败，不影响编辑器
  }
}

/** 自动保存：由 ChapterEditor change 事件触发。 */
async function handleAutoSave(content: string) {
  if (!chapter.value) return
  try {
    await saveChapter(chapter.value.id, {
      title: chapterTitle.value || undefined,
      content,
      status: chapter.value.status,
    })
    editorRef.value?.markSaved()
  } catch {
    editorRef.value?.markError()
  }
}

/** 保存标题（失去焦点时）。 */
async function handleTitleBlur() {
  if (!chapter.value) return
  try {
    await saveChapter(chapter.value.id, { title: chapterTitle.value })
    chapter.value.title = chapterTitle.value
  } catch {
    message.error('标题保存失败')
  }
}

onMounted(async () => {
  await loadOutline()
  if (chapterId) {
    await loadChapter(chapterId)
  }
})

watch(() => route.params.chapterId, async (newId) => {
  if (newId && typeof newId === 'string') {
    await loadChapter(newId)
  }
})
</script>

<template>
  <NLayout has-sider style="height: calc(100vh - 60px)">
    <!-- 章节大纲侧栏（P8-07 会扩展为完整侧栏，此处显示章节列表）-->
    <NLayoutSider
      bordered
      :width="220"
      style="overflow-y: auto; padding: 8px 0"
    >
      <template v-if="outline">
        <div v-for="vol in outline.volumes" :key="vol.id">
          <NText depth="3" style="font-size: 11px; padding: 6px 12px; display: block; font-weight: 600">
            {{ vol.title || '未命名卷' }}
          </NText>
          <div
            v-for="ch in vol.chapters"
            :key="ch.id"
            :class="['chapter-item', ch.id === chapterId ? 'active' : '']"
            @click="$router.push(`/projects/${projectId}/editor/${ch.id}`)"
          >
            <NText :depth="ch.id === chapterId ? 1 : 3" style="font-size: 13px">
              {{ ch.title || '未命名章节' }}
            </NText>
            <NText depth="3" style="font-size: 11px">{{ ch.wordCount }}字</NText>
          </div>
        </div>
      </template>
    </NLayoutSider>

    <!-- 编辑器主区 -->
    <NLayoutContent style="display: flex; flex-direction: column; overflow: hidden">
      <!-- 章节标题输入 -->
      <div style="padding: 16px 48px 8px; border-bottom: 1px solid #f0f0f0">
        <NInput
          v-model:value="chapterTitle"
          placeholder="章节标题"
          style="font-size: 22px; font-weight: 600; border: none"
          :bordered="false"
          @blur="handleTitleBlur"
        />
      </div>

      <!-- 加载中 -->
      <div v-if="loading" style="flex: 1; display: flex; align-items: center; justify-content: center">
        <NSpin size="large" />
      </div>

      <!-- 无章节时提示 -->
      <div v-else-if="!chapterId" style="flex: 1; display: flex; align-items: center; justify-content: center">
        <NEmpty description="请从左侧选择一个章节开始写作" />
      </div>

      <!-- TipTap 编辑器（只加载当前章节内容）-->
      <ChapterEditor
        v-else
        ref="editorRef"
        :chapter="chapter"
        style="flex: 1; overflow: hidden"
        @change="handleAutoSave"
      />
    </NLayoutContent>
  </NLayout>
</template>

<style scoped>
.chapter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 0;
  transition: background 0.1s;
}
.chapter-item:hover { background: rgba(0,0,0,0.04); }
.chapter-item.active { background: rgba(99, 125, 245, 0.1); }
</style>
