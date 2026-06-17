<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOpen,
  Boxes,
  Clapperboard,
  FileText,
  Library,
  Lock,
  PenLine,
  Plus,
  RefreshCw,
  Sparkles,
  Trash2,
  Unlock,
  Users,
} from 'lucide-vue-next'
import {
  createChapter,
  createProject,
  createVolume,
  deleteChapter,
  deleteProject,
  deleteVolume,
  getOutline,
  listProjects,
} from '@/api/project'
import type { OutlineInfo } from '@/api/project'
import type { ProjectInfo } from '@/api/types'
import {
  createCharacter,
  createWorldElement,
  deleteCharacter,
  deleteWorldElement,
  listCharacters,
  listWorldElements,
  toggleCharacterLock,
  updateCharacter,
  updateWorldElement,
} from '@/api/character'
import type { CharacterInfo, WorldElementInfo } from '@/api/character'
import {
  activateStyleTemplate,
  createStyleTemplate,
  deleteStyleTemplate,
  listStyleTemplates,
} from '@/api/styleTemplate'
import type { StyleTemplateInfo } from '@/api/styleTemplate'
import { buildPlotNodes, fallbackStyleTemplates, relationshipHints } from '@/mocks/wenshu'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()

const activeTab = ref('works')
const loading = ref(false)
const projects = ref<ProjectInfo[]>([])
const selectedProjectId = ref('')
const characters = ref<CharacterInfo[]>([])
const worldElements = ref<WorldElementInfo[]>([])
const outline = ref<OutlineInfo | null>(null)
const templates = ref<StyleTemplateInfo[]>([])
const templateFallback = ref(false)

const showProjectModal = ref(false)
const showCharacterModal = ref(false)
const showWorldModal = ref(false)
const showTemplateModal = ref(false)
const projectForm = reactive({ title: '', genre: '', synopsis: '', worldview: '' })
const characterForm = reactive({ id: '', name: '', role: '', personality: '', abilities: '', speechStyle: '' })
const worldForm = reactive({ id: '', type: 'dict', name: '', description: '', aliases: '' })
const templateForm = reactive({ name: '', type: 'web_novel', description: '', prompt: '' })
const volumeTitle = ref('')
const chapterTitle = ref('')

const tabs = [
  { key: 'works', label: '作品', icon: BookOpen },
  { key: 'characters', label: '角色库', icon: Users },
  { key: 'dict', label: '词典', icon: Library },
  { key: 'world', label: '世界观', icon: Boxes },
  { key: 'outline', label: '大纲', icon: FileText },
  { key: 'templates', label: '风格模板', icon: Sparkles },
]

const selectedProject = computed(() => projects.value.find((item) => item.id === selectedProjectId.value) || null)
const dictElements = computed(() => worldElements.value.filter((item) => (item.type || '').includes('dict') || item.type === 'term'))
const worldviewElements = computed(() => worldElements.value.filter((item) => !dictElements.value.includes(item)))
const totalWords = computed(() => projects.value.reduce((sum, item) => sum + (item.totalWords || 0), 0))

onMounted(loadProjects)
watch(selectedProjectId, () => loadProjectAssets(), { flush: 'post' })

function messageOf(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}

function formatWords(n = 0) {
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万` : String(n)
}

async function loadProjects() {
  loading.value = true
  try {
    const res = await listProjects()
    projects.value = res.data.data
    selectedProjectId.value = projects.value[0]?.id || ''
  } catch (error) {
    toast.error(messageOf(error, '作品列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadProjectAssets() {
  if (!selectedProjectId.value) return
  await Promise.allSettled([loadCharacters(), loadWorldElements(), loadOutline(), loadTemplates()])
}

async function loadCharacters() {
  if (!selectedProjectId.value) return
  const res = await listCharacters(selectedProjectId.value)
  characters.value = res.data.data
}

async function loadWorldElements() {
  if (!selectedProjectId.value) return
  const res = await listWorldElements(selectedProjectId.value)
  worldElements.value = res.data.data
}

async function loadOutline() {
  if (!selectedProjectId.value) return
  const res = await getOutline(selectedProjectId.value)
  outline.value = res.data.data
}

async function loadTemplates() {
  try {
    const res = await listStyleTemplates()
    templates.value = res.data.data
    templateFallback.value = false
  } catch {
    templates.value = fallbackStyleTemplates
    templateFallback.value = true
  }
}

async function saveProject() {
  if (!projectForm.title.trim()) return
  try {
    const res = await createProject({
      title: projectForm.title.trim(),
      genre: projectForm.genre || undefined,
      synopsis: projectForm.synopsis || undefined,
      worldview: projectForm.worldview || undefined,
    })
    projects.value.unshift(res.data.data)
    selectedProjectId.value = res.data.data.id
    Object.assign(projectForm, { title: '', genre: '', synopsis: '', worldview: '' })
    showProjectModal.value = false
    toast.success('作品已创建')
  } catch (error) {
    toast.error(messageOf(error, '创建作品失败'))
  }
}

async function removeProject(project: ProjectInfo) {
  if (!window.confirm(`确定删除「${project.title}」吗？`)) return
  try {
    await deleteProject(project.id)
    projects.value = projects.value.filter((item) => item.id !== project.id)
    selectedProjectId.value = projects.value[0]?.id || ''
    toast.success('作品已删除')
  } catch (error) {
    toast.error(messageOf(error, '删除失败'))
  }
}

function openEditor(project: ProjectInfo) {
  const firstChapter = outline.value?.volumes.flatMap((volume) => volume.chapters)[0]
  router.push(firstChapter ? `/projects/${project.id}/editor/${firstChapter.id}` : `/projects/${project.id}/editor`)
}

function editCharacter(item?: CharacterInfo) {
  Object.assign(characterForm, {
    id: item?.id || '',
    name: item?.name || '',
    role: item?.role || '',
    personality: item?.personality || '',
    abilities: item?.abilities || '',
    speechStyle: item?.speechStyle || '',
  })
  showCharacterModal.value = true
}

async function saveCharacter() {
  if (!selectedProjectId.value || !characterForm.name.trim()) return
  try {
    const payload = {
      name: characterForm.name.trim(),
      role: characterForm.role,
      personality: characterForm.personality,
      abilities: characterForm.abilities,
      speechStyle: characterForm.speechStyle,
    }
    if (characterForm.id) await updateCharacter(characterForm.id, payload)
    else await createCharacter(selectedProjectId.value, payload)
    showCharacterModal.value = false
    await loadCharacters()
    toast.success('角色已保存')
  } catch (error) {
    toast.error(messageOf(error, '角色保存失败'))
  }
}

async function removeCharacter(id: string) {
  if (!window.confirm('确定删除该角色吗？')) return
  await deleteCharacter(id)
  characters.value = characters.value.filter((item) => item.id !== id)
}

function editWorld(type: string, item?: WorldElementInfo) {
  Object.assign(worldForm, {
    id: item?.id || '',
    type: item?.type || type,
    name: item?.name || '',
    description: item?.description || '',
    aliases: item?.aliases?.join('、') || '',
  })
  showWorldModal.value = true
}

async function saveWorld() {
  if (!selectedProjectId.value || !worldForm.name.trim()) return
  const payload = {
    type: worldForm.type,
    name: worldForm.name.trim(),
    description: worldForm.description,
    aliases: worldForm.aliases.split(/[、,，]/).map((item) => item.trim()).filter(Boolean),
  }
  try {
    if (worldForm.id) await updateWorldElement(worldForm.id, payload)
    else await createWorldElement(selectedProjectId.value, payload)
    showWorldModal.value = false
    await loadWorldElements()
    toast.success('条目已保存')
  } catch (error) {
    toast.error(messageOf(error, '条目保存失败'))
  }
}

async function removeWorld(id: string) {
  if (!window.confirm('确定删除该条目吗？')) return
  await deleteWorldElement(id)
  worldElements.value = worldElements.value.filter((item) => item.id !== id)
}

async function addVolume() {
  if (!selectedProjectId.value || !volumeTitle.value.trim()) return
  await createVolume(selectedProjectId.value, { title: volumeTitle.value.trim() })
  volumeTitle.value = ''
  await loadOutline()
}

async function addChapter(volumeId: string) {
  if (!chapterTitle.value.trim()) return
  await createChapter(volumeId, { title: chapterTitle.value.trim() })
  chapterTitle.value = ''
  await loadOutline()
}

async function saveTemplate() {
  const payload = {
    name: templateForm.name.trim(),
    type: templateForm.type,
    description: templateForm.description,
    prompt: templateForm.prompt,
  }
  if (!payload.name) return
  if (templateFallback.value) {
    templates.value.unshift({ id: `mock-${Date.now()}`, ...payload, active: false })
    showTemplateModal.value = false
    return
  }
  await createStyleTemplate(payload)
  showTemplateModal.value = false
  await loadTemplates()
}

async function activateTemplate(item: StyleTemplateInfo) {
  if (templateFallback.value) {
    templates.value = templates.value.map((tpl) => ({ ...tpl, active: tpl.type === item.type ? tpl.id === item.id : tpl.active }))
    return
  }
  await activateStyleTemplate(item.id)
  await loadTemplates()
}

async function removeTemplate(item: StyleTemplateInfo) {
  if (templateFallback.value) {
    templates.value = templates.value.filter((tpl) => tpl.id !== item.id)
    return
  }
  await deleteStyleTemplate(item.id)
  await loadTemplates()
}
</script>

<template>
  <div class="ws-page">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Wenshu Workspace</p>
        <h1>创作中枢</h1>
        <p>围绕作品组织角色、词典、世界观、大纲和风格模板。</p>
      </div>
      <button class="ws-button ws-button--primary" type="button" @click="showProjectModal = true">
        <Plus :size="18" />
        新建作品
      </button>
    </section>

    <section class="ws-stat-grid">
      <div class="ws-card ws-stat">
        <BookOpen :size="19" />
        <strong>{{ projects.length }}</strong>
        <span>作品</span>
      </div>
      <div class="ws-card ws-stat">
        <PenLine :size="19" />
        <strong>{{ formatWords(totalWords) }}</strong>
        <span>总字数</span>
      </div>
      <div class="ws-card ws-stat">
        <Users :size="19" />
        <strong>{{ characters.length }}</strong>
        <span>当前作品角色</span>
      </div>
    </section>

    <div class="ws-toolbar">
      <div class="ws-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <component :is="tab.icon" :size="16" />
          {{ tab.label }}
        </button>
      </div>
      <select v-model="selectedProjectId" class="ws-select">
        <option value="">选择作品</option>
        <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.title }}</option>
      </select>
    </div>

    <div v-if="loading" class="ws-empty">
      <RefreshCw class="ws-spin" :size="28" />
      <span>正在加载工作台</span>
    </div>

    <section v-else-if="activeTab === 'works'" class="ws-grid">
      <article v-for="project in projects" :key="project.id" class="ws-card project-card">
        <div>
          <span class="ws-badge">{{ project.genre || '未分类' }}</span>
          <h3>{{ project.title }}</h3>
          <p>{{ project.synopsis || '暂未填写简介。' }}</p>
        </div>
        <div class="project-card__meta">
          <span>{{ formatWords(project.totalWords) }} 字</span>
          <span>{{ new Date(project.updatedAt).toLocaleDateString('zh-CN') }}</span>
        </div>
        <div class="ws-actions">
          <button class="ws-button ws-button--primary" type="button" @click="openEditor(project)">进入编辑器</button>
          <button class="ws-button" type="button" @click="router.push(`/projects/${project.id}/script`)">
            <Clapperboard :size="16" />
            剧本
          </button>
          <button class="ws-icon-button danger" type="button" title="删除" @click="removeProject(project)">
            <Trash2 :size="16" />
          </button>
        </div>
      </article>
      <button class="ws-card add-card" type="button" @click="showProjectModal = true">
        <Plus :size="24" />
        <strong>新建作品</strong>
      </button>
    </section>

    <section v-else-if="activeTab === 'characters'" class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2>角色库</h2>
          <p>{{ selectedProject?.title || '请选择作品' }}</p>
        </div>
        <button class="ws-button ws-button--primary" :disabled="!selectedProjectId" type="button" @click="editCharacter()">
          <Plus :size="16" />
          新增角色
        </button>
      </div>
      <div class="ws-grid">
        <article v-for="item in characters" :key="item.id" class="ws-card compact-card">
          <div class="compact-card__title">
            <h3>{{ item.name }}</h3>
            <button class="ws-icon-button" type="button" title="锁定" @click="toggleCharacterLock(item.id).then(loadCharacters)">
              <Lock v-if="item.locked" :size="16" />
              <Unlock v-else :size="16" />
            </button>
          </div>
          <span class="ws-badge">{{ item.role || '角色' }}</span>
          <p>{{ item.personality || item.appearance || '暂无角色描述。' }}</p>
          <div class="ws-actions">
            <button class="ws-button" type="button" @click="editCharacter(item)">编辑</button>
            <button class="ws-icon-button danger" type="button" title="删除" @click="removeCharacter(item.id)">
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </div>
      <p class="ws-hint">{{ relationshipHints[0] }}</p>
    </section>

    <section v-else-if="activeTab === 'dict' || activeTab === 'world'" class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2>{{ activeTab === 'dict' ? '词典' : '世界观' }}</h2>
          <p>{{ activeTab === 'dict' ? '术语、设定名词和别名。' : '势力、地点、规则和历史。' }}</p>
        </div>
        <button
          class="ws-button ws-button--primary"
          :disabled="!selectedProjectId"
          type="button"
          @click="editWorld(activeTab === 'dict' ? 'dict' : 'world')"
        >
          <Plus :size="16" />
          新增条目
        </button>
      </div>
      <div class="ws-grid">
        <article
          v-for="item in (activeTab === 'dict' ? dictElements : worldviewElements)"
          :key="item.id"
          class="ws-card compact-card"
        >
          <h3>{{ item.name }}</h3>
          <span class="ws-badge">{{ item.type || 'world' }}</span>
          <p>{{ item.description || '暂无描述。' }}</p>
          <small v-if="item.aliases?.length">别名：{{ item.aliases.join('、') }}</small>
          <div class="ws-actions">
            <button class="ws-button" type="button" @click="editWorld(activeTab === 'dict' ? 'dict' : 'world', item)">编辑</button>
            <button class="ws-icon-button danger" type="button" title="删除" @click="removeWorld(item.id)">
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === 'outline'" class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2>大纲</h2>
          <p>卷章结构连接真实接口，情节节点由章节梗概前端拆分展示。</p>
        </div>
        <div class="ws-inline">
          <input v-model="volumeTitle" class="ws-input" placeholder="新卷标题">
          <button class="ws-button ws-button--primary" type="button" @click="addVolume">新增卷</button>
        </div>
      </div>
      <div class="outline-list">
        <article v-for="volume in outline?.volumes || []" :key="volume.id" class="ws-card outline-volume">
          <div class="compact-card__title">
            <h3>{{ volume.title || '未命名卷' }}</h3>
            <button class="ws-icon-button danger" type="button" title="删除卷" @click="deleteVolume(volume.id).then(loadOutline)">
              <Trash2 :size="16" />
            </button>
          </div>
          <p>{{ volume.conflict || '尚未填写本卷核心冲突。' }}</p>
          <div class="chapter-list">
            <div v-for="chapter in volume.chapters" :key="chapter.id" class="chapter-row">
              <button type="button" @click="router.push(`/projects/${selectedProjectId}/editor/${chapter.id}`)">
                {{ chapter.title || '未命名章节' }}
              </button>
              <span>{{ chapter.wordCount || 0 }} 字</span>
              <button class="ws-icon-button danger" type="button" title="删除章节" @click="deleteChapter(chapter.id).then(loadOutline)">
                <Trash2 :size="14" />
              </button>
            </div>
          </div>
          <div class="plot-node-list">
            <span v-for="node in buildPlotNodes(volume.chapters[0]?.outline)" :key="node.id" class="plot-node">
              {{ node.summary }}
            </span>
          </div>
          <div class="ws-inline">
            <input v-model="chapterTitle" class="ws-input" placeholder="新章节标题">
            <button class="ws-button" type="button" @click="addChapter(volume.id)">新增章节</button>
          </div>
        </article>
      </div>
    </section>

    <section v-else class="ws-panel">
      <div class="ws-panel__head">
        <div>
          <h2>风格模板</h2>
          <p>{{ templateFallback ? '当前使用前端 mock 模板，接口恢复后自动切换真实数据。' : '同类型只保留一个激活模板。' }}</p>
        </div>
        <button class="ws-button ws-button--primary" type="button" @click="showTemplateModal = true">
          <Plus :size="16" />
          新增模板
        </button>
      </div>
      <div class="ws-grid">
        <article v-for="item in templates" :key="item.id" class="ws-card compact-card">
          <div class="compact-card__title">
            <h3>{{ item.name }}</h3>
            <span class="ws-badge" :class="{ success: item.active }">{{ item.active ? '使用中' : item.type }}</span>
          </div>
          <p>{{ item.description || item.prompt || '暂无描述。' }}</p>
          <div class="ws-actions">
            <button class="ws-button" type="button" @click="activateTemplate(item)">激活</button>
            <button class="ws-icon-button danger" type="button" title="删除" @click="removeTemplate(item)">
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="showProjectModal" class="ws-modal">
        <form class="ws-modal__panel ws-form" @submit.prevent="saveProject">
          <h2>新建作品</h2>
          <label class="ws-field"><span>标题</span><input v-model="projectForm.title" class="ws-input"></label>
          <label class="ws-field"><span>类型</span><input v-model="projectForm.genre" class="ws-input"></label>
          <label class="ws-field"><span>简介</span><textarea v-model="projectForm.synopsis" class="ws-textarea" /></label>
          <label class="ws-field"><span>世界观</span><textarea v-model="projectForm.worldview" class="ws-textarea" /></label>
          <div class="ws-actions right"><button class="ws-button" type="button" @click="showProjectModal = false">取消</button><button class="ws-button ws-button--primary">创建</button></div>
        </form>
      </div>

      <div v-if="showCharacterModal" class="ws-modal">
        <form class="ws-modal__panel ws-form" @submit.prevent="saveCharacter">
          <h2>{{ characterForm.id ? '编辑角色' : '新增角色' }}</h2>
          <label class="ws-field"><span>姓名</span><input v-model="characterForm.name" class="ws-input"></label>
          <label class="ws-field"><span>定位</span><input v-model="characterForm.role" class="ws-input"></label>
          <label class="ws-field"><span>性格</span><textarea v-model="characterForm.personality" class="ws-textarea" /></label>
          <label class="ws-field"><span>能力</span><textarea v-model="characterForm.abilities" class="ws-textarea" /></label>
          <label class="ws-field"><span>说话风格</span><input v-model="characterForm.speechStyle" class="ws-input"></label>
          <div class="ws-actions right"><button class="ws-button" type="button" @click="showCharacterModal = false">取消</button><button class="ws-button ws-button--primary">保存</button></div>
        </form>
      </div>

      <div v-if="showWorldModal" class="ws-modal">
        <form class="ws-modal__panel ws-form" @submit.prevent="saveWorld">
          <h2>条目</h2>
          <label class="ws-field"><span>类型</span><input v-model="worldForm.type" class="ws-input"></label>
          <label class="ws-field"><span>名称</span><input v-model="worldForm.name" class="ws-input"></label>
          <label class="ws-field"><span>描述</span><textarea v-model="worldForm.description" class="ws-textarea" /></label>
          <label class="ws-field"><span>别名</span><input v-model="worldForm.aliases" class="ws-input" placeholder="用顿号或逗号分隔"></label>
          <div class="ws-actions right"><button class="ws-button" type="button" @click="showWorldModal = false">取消</button><button class="ws-button ws-button--primary">保存</button></div>
        </form>
      </div>

      <div v-if="showTemplateModal" class="ws-modal">
        <form class="ws-modal__panel ws-form" @submit.prevent="saveTemplate">
          <h2>风格模板</h2>
          <label class="ws-field"><span>名称</span><input v-model="templateForm.name" class="ws-input"></label>
          <label class="ws-field"><span>类型</span><input v-model="templateForm.type" class="ws-input"></label>
          <label class="ws-field"><span>描述</span><textarea v-model="templateForm.description" class="ws-textarea" /></label>
          <label class="ws-field"><span>Prompt</span><textarea v-model="templateForm.prompt" class="ws-textarea" /></label>
          <div class="ws-actions right"><button class="ws-button" type="button" @click="showTemplateModal = false">取消</button><button class="ws-button ws-button--primary">保存</button></div>
        </form>
      </div>
    </Teleport>
  </div>
</template>
