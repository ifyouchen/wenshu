<script setup lang="ts">
/**
 * TipTap 章节编辑器（P8-06）。
 * 核心约束：
 *  - 任意时刻只加载当前章节内容，不加载全卷。
 *  - 内容变更自动保存（debounce 1000ms）。
 *  - 字数统计使用 CharacterCount extension。
 */
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import { NSpace, NText, NTag } from 'naive-ui'
import type { ChapterInfo } from '@/api/project'

const props = defineProps<{
  /** 当前章节数据（由父组件通过 API 加载后传入，编辑器不自行加载卷数据）。 */
  chapter: ChapterInfo | null
  /** 是否只读。 */
  readonly?: boolean
}>()

const emit = defineEmits<{
  /** 内容发生变化，携带新的 HTML 内容（debounced）。 */
  change: [content: string]
}>()

/** 自动保存防抖定时器。 */
let saveTimer: ReturnType<typeof setTimeout> | null = null
/** 保存状态：idle / saving / saved / error */
const saveStatus = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      // 仅保留必要格式，网文写作不需要标题/代码块等
      heading: false,
      codeBlock: false,
      blockquote: false,
    }),
    Placeholder.configure({ placeholder: '开始写作……' }),
    CharacterCount,
  ],
  editable: !props.readonly,
  content: props.chapter?.content ?? '',
  onUpdate: ({ editor }) => {
    // 内容变化时触发防抖保存
    if (saveTimer) clearTimeout(saveTimer)
    saveStatus.value = 'idle'
    saveTimer = setTimeout(() => {
      emit('change', editor.getHTML())
      saveStatus.value = 'saving'
    }, 1000)
  },
})

/** 章节切换时更新编辑器内容（只加载新章节，不加载全卷）。 */
watch(() => props.chapter, (newChapter) => {
  if (!editor.value || !newChapter) return
  const current = editor.value.getHTML()
  const incoming = newChapter.content ?? ''
  if (current !== incoming) {
    editor.value.commands.setContent(incoming)
    saveStatus.value = 'idle'
  }
}, { immediate: false })

/** 父组件可通过此方法标记保存结果。 */
function markSaved() { saveStatus.value = 'saved' }
function markError() { saveStatus.value = 'error' }

defineExpose({ markSaved, markError })

onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
  editor.value?.destroy()
})
</script>

<template>
  <div class="chapter-editor">
    <!-- 状态栏：字数统计 + 保存状态 -->
    <div class="editor-statusbar">
      <NSpace align="center" :size="12">
        <NText depth="3" style="font-size: 12px">
          {{ editor?.storage.characterCount.words() ?? 0 }} 词 ·
          {{ editor?.storage.characterCount.characters() ?? 0 }} 字
        </NText>
        <NTag
          v-if="saveStatus !== 'idle'"
          :type="saveStatus === 'saved' ? 'success' : saveStatus === 'error' ? 'error' : 'default'"
          size="small"
          :bordered="false"
        >
          {{ saveStatus === 'saving' ? '保存中…' : saveStatus === 'saved' ? '已保存' : '保存失败' }}
        </NTag>
      </NSpace>
    </div>

    <!-- TipTap 编辑区 -->
    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<style scoped>
.chapter-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.editor-statusbar {
  padding: 4px 16px;
  border-bottom: 1px solid var(--n-border-color, #eee);
  background: var(--n-color, #fff);
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 48px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

/* TipTap prose 样式 */
:deep(.ProseMirror) {
  min-height: 400px;
  outline: none;
  font-size: 16px;
  line-height: 1.8;
  color: #1a1a1a;
}

:deep(.ProseMirror p) {
  margin: 0 0 8px;
  text-indent: 2em;
}

:deep(.ProseMirror p.is-empty::before) {
  content: attr(data-placeholder);
  color: #aaa;
  pointer-events: none;
  float: left;
  height: 0;
}
</style>
