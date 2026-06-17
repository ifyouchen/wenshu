<script setup lang="ts">
/**
 * TipTap 章节编辑器。
 *
 * 核心约束：
 * - 任意时刻只加载当前章节内容，不加载全卷。
 * - 内容变更自动保存（debounce 1000ms）。
 * - 字数统计使用 CharacterCount extension。
 */
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import { NIcon } from 'naive-ui'
import { Check, AlertCircle, Loader2 } from 'lucide-vue-next'
import type { ChapterInfo } from '@/api/project'

const props = defineProps<{
  chapter: ChapterInfo | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  change: [content: string]
  editorReady: [editor: Editor]
}>()

let saveTimer: ReturnType<typeof setTimeout> | null = null
const saveStatus = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: false,
      codeBlock: false,
      blockquote: false,
    }),
    Placeholder.configure({ placeholder: '从这里继续写下去...' }),
    CharacterCount,
  ],
  editable: !props.readonly,
  content: props.chapter?.content ?? '',
  onUpdate: ({ editor }) => {
    if (saveTimer) clearTimeout(saveTimer)
    saveStatus.value = 'idle'
    saveTimer = setTimeout(() => {
      emit('change', editor.getHTML())
      saveStatus.value = 'saving'
    }, 1000)
  },
})

watch(() => props.chapter, (newChapter) => {
  if (!editor.value || !newChapter) return
  const current = editor.value.getHTML()
  const incoming = newChapter.content ?? ''
  if (current !== incoming) {
    editor.value.commands.setContent(incoming)
    saveStatus.value = 'idle'
  }
}, { immediate: false })

function markSaved() { saveStatus.value = 'saved' }
function markError() { saveStatus.value = 'error' }

defineExpose({ markSaved, markError })

onMounted(() => {
  if (editor.value) emit('editorReady', editor.value)
})

onBeforeUnmount(() => {
  if (saveTimer) clearTimeout(saveTimer)
  editor.value?.destroy()
})
</script>

<template>
  <div class="chapter-editor">
    <!-- 状态栏 -->
    <div class="editor-statusbar">
      <div class="statusbar-left">
        <span class="statusbar-stat">
          {{ editor?.storage.characterCount.words() ?? 0 }} 词
        </span>
        <span class="statusbar-divider" />
        <span class="statusbar-stat">
          {{ editor?.storage.characterCount.characters() ?? 0 }} 字
        </span>
      </div>
      <div class="statusbar-right">
        <span v-if="saveStatus === 'saving'" class="save-status saving">
          <NIcon :component="Loader2" :size="12" class="spin-icon" />
          保存中
        </span>
        <span v-else-if="saveStatus === 'saved'" class="save-status saved">
          <NIcon :component="Check" :size="12" />
          已保存
        </span>
        <span v-else-if="saveStatus === 'error'" class="save-status error">
          <NIcon :component="AlertCircle" :size="12" />
          保存失败
        </span>
      </div>
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
  background: var(--w-bg);
}

.editor-statusbar {
  height: 36px;
  padding: 0 var(--w-space-5);
  border-bottom: 1px solid var(--w-border-subtle);
  background: var(--w-bg-toolbar);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

.statusbar-left {
  display: flex;
  align-items: center;
  gap: var(--w-space-2);
}

.statusbar-divider {
  width: 1px;
  height: 12px;
  background: var(--w-border-default);
}

.statusbar-stat {
  font-variant-numeric: tabular-nums;
}

.save-status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.save-status.saved {
  color: var(--w-success);
}

.save-status.error {
  color: var(--w-danger);
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--w-space-6) var(--w-space-5) var(--w-space-8);
}

/* TipTap prose 样式 */
:deep(.ProseMirror) {
  max-width: var(--w-editor-max-width);
  margin: 0 auto;
  min-height: min(760px, calc(100vh - 210px));
  outline: none;
  font-size: 17px;
  line-height: 1.85;
  color: var(--w-text);
  background: var(--w-bg-paper);
  border: 1px solid var(--w-border-subtle);
  border-radius: var(--w-radius-lg);
  padding: clamp(28px, 5vw, 56px);
  box-shadow: var(--w-shadow-sm);
}

:deep(.ProseMirror p) {
  margin: 0 0 14px;
  text-indent: 2em;
}

:deep(.ProseMirror p.is-empty::before) {
  content: attr(data-placeholder);
  color: var(--w-text-tertiary);
  pointer-events: none;
  float: left;
  height: 0;
  text-indent: 2em;
}

:deep(.ProseMirror p.is-empty:last-child::before) {
  text-indent: 2em;
}

/* 生成内容标识：左侧细线，默认隐藏 */
:deep([data-ai="true"]) {
  position: relative;
  border-left: 2px solid var(--w-ai-line);
  padding-left: 12px;
  margin-left: -14px;
  background: linear-gradient(90deg, var(--w-ai-bg), transparent 64%);
  border-radius: 0 var(--w-radius-sm) var(--w-radius-sm) 0;
  transition: all var(--w-transition-base);
}

:deep([data-ai="true"]:hover) {
  background: var(--w-ai-bg);
}

/* 选中文字 */
:deep(.ProseMirror ::selection) {
  background: rgba(90, 110, 138, 0.35);
  color: var(--w-text);
}

@media (max-width: 767px) {
  .editor-content {
    padding: var(--w-space-4) var(--w-space-3);
  }

  :deep(.ProseMirror) {
    font-size: 16px;
    padding: var(--w-space-5) var(--w-space-4);
    border-radius: var(--w-radius-md);
  }
}
</style>
