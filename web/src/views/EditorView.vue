<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import {
  BookOpen,
  Boxes,
  CheckCircle2,
  Clapperboard,
  FileText,
  Library,
  MessageSquareText,
  PanelLeft,
  Save,
  Search,
  Settings,
  Users,
} from 'lucide-vue-next'
import { getChapter, getChapterContext, getOutline, saveChapter } from '@/api/project'
import type { ChapterInfo, OutlineInfo } from '@/api/project'
import { listCharacters, listWorldElements } from '@/api/character'
import type { CharacterInfo, WorldElementInfo } from '@/api/character'
import { polishAdvanced, polishBasic } from '@/api/polish'
import { submitConsistencyCheck } from '@/api/consistency'
import { continueNovel, createBranch } from '@/api/novel'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const projectId = computed(() => String(route.params.projectId))
const chapterId = computed(() => String(route.params.chapterId || ''))
const chapter = ref<ChapterInfo | null>(null)
const outline = ref<OutlineInfo | null>(null)
const characters = ref<CharacterInfo[]>([])
const worldElements = ref<WorldElementInfo[]>([])
const sideOpen = ref(true)
const sideMode = ref('outline')
const loading = ref(false)
const saveState = ref<'saved' | 'saving' | 'error'>('saved')
const title = ref('')
const command = ref('')
const aiMessages = ref<Array<{ role: 'user' | 'assistant'; text: string }>>([
  { role: 'assistant', text: '选用底部命令或直接输入需求：续写、润色、扩写、缩写、转剧本、查一致性。' },
])
let saveTimer: number | undefined

const editor = useEditor({
  extensions: [
    StarterKit,
    Placeholder.configure({ placeholder: '从这里继续写。文枢会自动保存章节内容。' }),
    CharacterCount,
  ],
  content: '',
  editorProps: {
    attributes: {
      class: 'wenshu-editor-prose',
    },
  },
  onUpdate: () => scheduleSave(),
})

const sideItems = [
  { key: 'outline', title: '章节大纲', icon: FileText },
  { key: 'characters', title: '角色', icon: Users },
  { key: 'dict', title: '词典', icon: Library },
  { key: 'world', title: '世界观', icon: Boxes },
  { key: 'search', title: '搜索', icon: Search },
  { key: 'settings', title: '设置', icon: Settings },
]

const wordCount = computed(() => editor.value?.storage.characterCount.characters() || 0)
const dictElements = computed(() => worldElements.value.filter((item) => (item.type || '').includes('dict') || item.type === 'term'))
const worldviewElements = computed(() => worldElements.value.filter((item) => !dictElements.value.includes(item)))

onMounted(loadAll)
onBeforeUnmount(() => {
  if (saveTimer) window.clearTimeout(saveTimer)
  editor.value?.destroy()
})
watch(chapterId, loadAll)

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

async function loadAll() {
  loading.value = true
  try {
    const outlineRes = await getOutline(projectId.value)
    outline.value = outlineRes.data.data
    if (!chapterId.value) return

    try {
      const contextRes = await getChapterContext(chapterId.value)
      chapter.value = contextRes.data.data.chapter
      outline.value = contextRes.data.data.outline || outline.value
      characters.value = contextRes.data.data.characters as CharacterInfo[]
      worldElements.value = contextRes.data.data.worldElements as WorldElementInfo[]
    } catch {
      const [chapterRes, characterRes, worldRes] = await Promise.all([
        getChapter(chapterId.value),
        listCharacters(projectId.value),
        listWorldElements(projectId.value),
      ])
      chapter.value = chapterRes.data.data
      characters.value = characterRes.data.data
      worldElements.value = worldRes.data.data
    }
    title.value = chapter.value?.title || ''
    editor.value?.commands.setContent(chapter.value?.content || '', { emitUpdate: false })
  } catch (error) {
    toast.error(messageOf(error, '编辑器加载失败'))
  } finally {
    loading.value = false
  }
}

function selectChapter(id: string) {
  router.push(`/projects/${projectId.value}/editor/${id}`)
}

function scheduleSave() {
  if (!chapter.value) return
  saveState.value = 'saving'
  if (saveTimer) window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(saveNow, 900)
}

async function saveNow() {
  if (!chapter.value) return
  try {
    const res = await saveChapter(chapter.value.id, {
      title: title.value,
      content: editor.value?.getHTML() || '',
      status: chapter.value.status,
    })
    chapter.value = res.data.data
    saveState.value = 'saved'
  } catch {
    saveState.value = 'error'
  }
}

async function saveTitle() {
  await saveNow()
}

async function runCommand(preset?: string) {
  const text = (preset || command.value).trim()
  if (!text) return
  command.value = ''
  aiMessages.value.push({ role: 'user', text })
  const lower = text.toLowerCase()

  try {
    if ((text.includes('续写') || lower.includes('continue')) && chapterId.value) {
      let result = ''
      aiMessages.value.push({ role: 'assistant', text: '正在续写...' })
      const index = aiMessages.value.length - 1
      await continueNovel(chapterId.value, (token) => {
        result += token
        aiMessages.value[index].text = result || '正在续写...'
      })
      return
    }
    if (text.includes('润色')) {
      const selected = editor.value?.state.doc.textBetween(
        editor.value.state.selection.from,
        editor.value.state.selection.to,
        '\n',
      )
      const source = selected || editor.value?.getText().slice(0, 1200) || ''
      const res = text.includes('高级') ? await polishAdvanced(source, text) : await polishBasic(source)
      aiMessages.value.push({ role: 'assistant', text: res.data.data.rewritten || res.data.data.basicAnnotations?.map((item) => `${item.original} -> ${item.suggested}`).join('\n') || '暂无润色建议' })
      return
    }
    if (text.includes('转剧本')) {
      router.push(`/projects/${projectId.value}/script`)
      return
    }
    if (text.includes('一致性')) {
      const res = await submitConsistencyCheck(projectId.value)
      router.push(`/consistency/reports/${res.data.data.reportId}`)
      return
    }
    if (text.includes('分支')) {
      const res = await createBranch(projectId.value, chapterId.value || undefined)
      aiMessages.value.push({ role: 'assistant', text: res.data.data.map((item) => `${item.title}\n${item.summary}`).join('\n\n') || '暂无分支建议' })
      return
    }
    aiMessages.value.push({ role: 'assistant', text: `已记录你的需求：「${text}」。当前后端没有对应命令接口，先以面板消息保留。` })
  } catch (error) {
    aiMessages.value.push({ role: 'assistant', text: messageOf(error, 'AI 能力暂时不可用，请检查模型配置或后端接口。') })
  }
}
</script>

<template>
  <div class="editor-shell" :class="{ 'side-collapsed': !sideOpen }">
    <aside class="editor-rail">
      <button class="ws-icon-button" type="button" title="展开侧栏" @click="sideOpen = !sideOpen">
        <PanelLeft :size="18" />
      </button>
      <button
        v-for="item in sideItems"
        :key="item.key"
        class="rail-button"
        :class="{ active: sideMode === item.key }"
        type="button"
        :title="item.title"
        @click="sideMode = item.key; sideOpen = true"
      >
        <component :is="item.icon" :size="18" />
      </button>
    </aside>

    <aside class="editor-side">
      <header><h2>{{ sideItems.find((item) => item.key === sideMode)?.title }}</h2></header>
      <div v-if="sideMode === 'outline'" class="side-list">
        <section v-for="volume in outline?.volumes || []" :key="volume.id">
          <h3>{{ volume.title || '未命名卷' }}</h3>
          <button
            v-for="item in volume.chapters"
            :key="item.id"
            type="button"
            :class="{ active: item.id === chapterId }"
            @click="selectChapter(item.id)"
          >
            {{ item.title || '未命名章节' }}
          </button>
        </section>
      </div>
      <div v-else-if="sideMode === 'characters'" class="side-list">
        <article v-for="item in characters" :key="item.id">
          <strong>{{ item.name }}</strong>
          <span>{{ item.role || '角色' }}</span>
          <p>{{ item.personality || item.speechStyle || '暂无描述' }}</p>
        </article>
      </div>
      <div v-else-if="sideMode === 'dict' || sideMode === 'world'" class="side-list">
        <article v-for="item in (sideMode === 'dict' ? dictElements : worldviewElements)" :key="item.id">
          <strong>{{ item.name }}</strong>
          <span>{{ item.type }}</span>
          <p>{{ item.description || '暂无描述' }}</p>
        </article>
      </div>
      <div v-else class="ws-empty compact">
        <span>{{ sideMode === 'search' ? '搜索面板后续接全书搜索接口。' : '编辑设置已集中到顶部保存与底部命令栏。' }}</span>
      </div>
    </aside>

    <main class="editor-center">
      <header class="editor-titlebar">
        <input v-model="title" class="editor-title-input" placeholder="章节标题" @blur="saveTitle">
        <div class="save-state" :class="saveState">
          <Save v-if="saveState === 'saving'" :size="15" />
          <CheckCircle2 v-else :size="15" />
          {{ saveState === 'saving' ? '保存中' : saveState === 'error' ? '保存失败' : '已保存' }} · {{ wordCount }} 字
        </div>
      </header>

      <section v-if="loading" class="ws-empty"><span>章节加载中...</span></section>
      <section v-else-if="!chapterId" class="ws-empty">
        <BookOpen :size="32" />
        <span>从左侧选择章节开始写作。</span>
      </section>
      <section v-else class="editor-paper">
        <EditorContent :editor="editor" />
      </section>

      <footer class="command-bar">
        <button type="button" @click="runCommand('续写')">续写</button>
        <button type="button" @click="runCommand('润色')">润色</button>
        <button type="button" @click="runCommand('扩写')">扩写</button>
        <button type="button" @click="runCommand('缩写')">缩写</button>
        <button type="button" @click="runCommand('转剧本')">转剧本</button>
        <button type="button" @click="runCommand('查一致性')">查一致性</button>
        <input v-model="command" placeholder="输入命令或需求" @keydown.enter.prevent="runCommand()">
        <button type="button" class="primary" @click="runCommand()">发送</button>
      </footer>
    </main>

    <aside class="ai-panel">
      <header>
        <MessageSquareText :size="18" />
        <h2>AI 助手</h2>
      </header>
      <div class="ai-stream">
        <div v-for="(item, index) in aiMessages" :key="index" class="ai-message" :class="item.role">
          {{ item.text }}
        </div>
      </div>
      <div class="ai-actions">
        <button class="ws-button" type="button" @click="runCommand('分支建议')">剧情分支</button>
        <button class="ws-button" type="button" @click="runCommand('转剧本')">
          <Clapperboard :size="15" />
          转剧本
        </button>
      </div>
    </aside>
  </div>
</template>
