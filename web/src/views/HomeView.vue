<script setup lang="ts">
/**
 * 作品工作台：搜索、排序、继续写作、新建和导入入口。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst } from 'naive-ui'
import {
  NButton,
  NDropdown,
  NEllipsis,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NSpin,
  useDialog,
  useMessage,
} from 'naive-ui'
import {
  ArrowRight,
  BookOpen,
  Clock,
  FileText,
  FileUp,
  MoreHorizontal,
  PenLine,
  Plus,
  Search,
  SlidersHorizontal,
} from 'lucide-vue-next'
import { createProject, deleteProject, listProjects } from '@/api/project'
import type { ProjectInfo } from '@/api/types'
import OnboardingHint from '@/components/OnboardingHint.vue'
import QuotaTooltip from '@/components/QuotaTooltip.vue'
import { useOnboarding } from '@/composables/useOnboarding'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const ob = useOnboarding()

const projects = ref<ProjectInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInst | null>(null)
const searchText = ref('')
const sortKey = ref<'updated' | 'created' | 'words' | 'genre'>('updated')
const showWelcomeHint = ref(false)

const createForm = reactive({ title: '', genre: '', synopsis: '' })

const totalWords = computed(() => projects.value.reduce((sum, project) => sum + (project.totalWords || 0), 0))
const latestProject = computed(() => sortedProjects.value[0])

const sortOptions = [
  { label: '最近修改', value: 'updated' },
  { label: '创建时间', value: 'created' },
  { label: '字数多少', value: 'words' },
  { label: '类型分组', value: 'genre' },
]

const genreOptions = [
  { label: '玄幻', value: '玄幻' },
  { label: '仙侠', value: '仙侠' },
  { label: '都市', value: '都市' },
  { label: '言情', value: '言情' },
  { label: '历史', value: '历史' },
  { label: '科幻', value: '科幻' },
  { label: '悬疑', value: '悬疑' },
  { label: '武侠', value: '武侠' },
  { label: '短剧', value: '短剧' },
  { label: '其他', value: '其他' },
]

const genreColors: Record<string, string> = {
  '玄幻': '#7967a0',
  '仙侠': '#6f8f8d',
  '都市': '#647fa1',
  '言情': '#a36b79',
  '历史': '#927650',
  '科幻': '#5d8a9b',
  '悬疑': '#71747b',
  '武侠': '#60846b',
  '短剧': '#9a6e88',
  '其他': 'var(--w-brand)',
}

const createRules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur', min: 1, max: 100 }],
}

const sortedProjects = computed(() => {
  const q = searchText.value.trim().toLowerCase()
  const list = projects.value
    .filter(project => {
      if (!q) return true
      return project.title.toLowerCase().includes(q) ||
        project.genre?.toLowerCase().includes(q) ||
        project.synopsis?.toLowerCase().includes(q)
    })
    .slice()

  return list.sort((a, b) => {
    if (sortKey.value === 'created') return Date.parse(b.createdAt) - Date.parse(a.createdAt)
    if (sortKey.value === 'words') return (b.totalWords || 0) - (a.totalWords || 0)
    if (sortKey.value === 'genre') return (a.genre || '其他').localeCompare(b.genre || '其他', 'zh-CN')
    return Date.parse(b.updatedAt) - Date.parse(a.updatedAt)
  })
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await listProjects()
    projects.value = res.data.data
    if (projects.value.length === 0 && ob.shouldShow('first-home')) {
      showWelcomeHint.value = true
    }
  } catch {
    message.error('作品列表加载失败')
  } finally {
    loading.value = false
  }
})

async function handleCreate() {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    const res = await createProject({
      title: createForm.title,
      genre: createForm.genre || undefined,
      synopsis: createForm.synopsis || undefined,
    })
    projects.value.unshift(res.data.data)
    showCreate.value = false
    Object.assign(createForm, { title: '', genre: '', synopsis: '' })
    message.success('作品已创建')
  } catch {
    message.error('创建失败，请重试')
  } finally {
    createLoading.value = false
  }
}

function openProject(projectId: string, importMode = false) {
  router.push({
    path: `/projects/${projectId}/editor`,
    query: importMode ? { import: '1' } : undefined,
  })
}

function openLatestImport() {
  if (latestProject.value) {
    openProject(latestProject.value.id, true)
    return
  }
  showCreate.value = true
  message.info('请先创建作品，再在编辑器中导入已有稿件')
}

function handleCardAction(key: string, project: ProjectInfo) {
  if (key === 'edit') {
    openProject(project.id)
  } else if (key === 'script') {
    router.push(`/projects/${project.id}/script`)
  } else if (key === 'import') {
    openProject(project.id, true)
  } else if (key === 'delete') {
    dialog.warning({
      title: '删除作品',
      content: `确定要删除「${project.title}」吗？此操作不可恢复。`,
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await deleteProject(project.id)
          projects.value = projects.value.filter(p => p.id !== project.id)
          message.success('已删除')
        } catch {
          message.error('删除失败')
        }
      },
    })
  }
}

function formatWords(n: number) {
  return n >= 10000 ? `${(n / 10000).toFixed(1)}万字` : `${n}字`
}

function formatDate(d: string) {
  const date = new Date(d)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 30) return `${days} 天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="home-page">
    <div class="w-container">
      <section class="home-toolbar">
        <div>
          <p class="w-eyebrow">Works</p>
          <h1 class="w-title">作品工作台</h1>
          <p class="w-subtitle">管理长篇、导入存稿、继续最近章节。</p>
        </div>

        <div class="toolbar-actions">
          <QuotaTooltip />
          <NButton secondary class="toolbar-btn" @click="openLatestImport">
            <template #icon>
              <NIcon :component="FileUp" :size="16" />
            </template>
            导入稿件
          </NButton>
          <NButton type="primary" class="toolbar-btn" @click="showCreate = true">
            <template #icon>
              <NIcon :component="Plus" :size="16" />
            </template>
            新建作品
          </NButton>
        </div>
      </section>

      <section class="workspace-strip">
        <button
          class="workspace-stat workspace-stat--action"
          :disabled="!latestProject"
          @click="latestProject && openProject(latestProject.id)"
        >
          <NIcon :component="Clock" :size="18" />
          <span>
            <strong>{{ latestProject?.title || '暂无最近作品' }}</strong>
            <small>继续写作</small>
          </span>
          <NIcon :component="ArrowRight" :size="16" class="stat-arrow" />
        </button>
        <div class="workspace-stat">
          <NIcon :component="BookOpen" :size="18" />
          <span>
            <strong>{{ projects.length }}</strong>
            <small>作品数量</small>
          </span>
        </div>
        <div class="workspace-stat">
          <NIcon :component="PenLine" :size="18" />
          <span>
            <strong>{{ formatWords(totalWords) }}</strong>
            <small>累计正文</small>
          </span>
        </div>
      </section>

      <section class="filter-bar">
        <div class="search-box">
          <NIcon :component="Search" :size="16" />
          <input v-model="searchText" placeholder="搜索作品、类型或简介" />
        </div>
        <div class="sort-box">
          <NIcon :component="SlidersHorizontal" :size="15" />
          <NSelect v-model:value="sortKey" :options="sortOptions" size="small" class="sort-select" />
        </div>
      </section>

      <OnboardingHint
        v-if="showWelcomeHint"
        icon="info"
        title="建立你的写作空间"
        description="先创建一个作品，或导入已有稿件。后续的章节、角色、词典、统计和剧本改编都会围绕作品沉淀。"
        action-label="创建作品"
        variant="welcome"
        @close="showWelcomeHint = false; ob.markDone('first-home')"
        @action="showWelcomeHint = false; ob.markDone('first-home'); showCreate = true"
      />

      <div v-if="loading" class="loading-box">
        <NSpin size="large" />
      </div>

      <section v-else-if="!projects.length" class="empty-workbench">
        <div class="empty-illustration">
          <NIcon :component="FileText" :size="42" />
        </div>
        <h3>还没有作品</h3>
        <p>从空白作品开始，或先建立作品后导入已有稿件。</p>
        <div class="empty-actions">
          <NButton type="primary" @click="showCreate = true">
            <template #icon>
              <NIcon :component="Plus" :size="16" />
            </template>
            创建空白作品
          </NButton>
          <NButton secondary @click="showCreate = true">
            <template #icon>
              <NIcon :component="FileUp" :size="16" />
            </template>
            准备导入稿件
          </NButton>
        </div>
      </section>

      <section v-else class="project-grid">
        <article
          v-for="project in sortedProjects"
          :key="project.id"
          class="project-card"
          @click="openProject(project.id)"
        >
          <div
            class="project-card-accent"
            :style="{ background: genreColors[project.genre || '其他'] || genreColors['其他'] }"
          />
          <div class="project-card-header">
            <div class="project-title-block">
              <NEllipsis class="project-title">
                {{ project.title }}
              </NEllipsis>
              <span class="project-genre">
                {{ project.genre || '未分类' }}
              </span>
            </div>

            <NDropdown
              trigger="click"
              :options="[
                { label: '继续写作', key: 'edit' },
                { label: '导入内容', key: 'import' },
                { label: '改编剧本', key: 'script' },
                { type: 'divider', key: 'divider' },
                { label: '删除作品', key: 'delete', props: { style: 'color: var(--w-danger)' } },
              ]"
              @select="(k) => handleCardAction(k, project)"
              @click.stop
            >
              <button class="project-card-more" @click.stop>
                <NIcon :component="MoreHorizontal" :size="16" />
              </button>
            </NDropdown>
          </div>

          <p class="project-desc">
            {{ project.synopsis || '暂未填写简介。打开作品后可继续写作、维护大纲和角色资料。' }}
          </p>

          <div class="project-meta">
            <span>{{ formatWords(project.totalWords) }}</span>
            <span>{{ formatDate(project.updatedAt) }}修改</span>
          </div>

          <div class="project-actions" @click.stop>
            <NButton size="small" type="primary" @click="openProject(project.id)">
              继续写作
            </NButton>
            <NButton size="small" secondary @click="router.push(`/projects/${project.id}/script`)">
              改编
            </NButton>
          </div>
        </article>

        <button class="project-card project-card--new" @click="showCreate = true">
          <NIcon :component="Plus" :size="24" />
          <strong>新建作品</strong>
          <span>创建新的创作项目</span>
        </button>
      </section>
    </div>

    <NModal v-model:show="showCreate" preset="card" title="新建作品" style="width: min(480px, 96vw)">
      <NForm ref="createFormRef" :model="createForm" :rules="createRules">
        <NFormItem label="作品名称 *" path="title">
          <NInput v-model:value="createForm.title" placeholder="例如：长夜将明" />
        </NFormItem>
        <NFormItem label="类型">
          <NSelect
            v-model:value="createForm.genre"
            :options="genreOptions"
            placeholder="选择类型（可选）"
            clearable
          />
        </NFormItem>
        <NFormItem label="简介">
          <NInput
            v-model:value="createForm.synopsis"
            type="textarea"
            placeholder="一句话记录故事方向，后续可随时修改。"
            :rows="3"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showCreate = false">取消</NButton>
          <NButton type="primary" :loading="createLoading" @click="handleCreate">创建</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.home-page {
  min-height: calc(100vh - var(--w-topbar-height));
  padding: var(--w-space-6) 0 calc(var(--w-space-8) + var(--w-mobile-nav-height));
  overflow-y: auto;
  background: var(--w-bg);
}

.home-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--w-space-4);
  margin-bottom: var(--w-space-5);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  flex-wrap: wrap;
}

.toolbar-btn {
  min-height: 38px;
}

.workspace-strip {
  display: grid;
  grid-template-columns: 1.5fr repeat(2, minmax(0, 1fr));
  gap: var(--w-space-3);
  margin-bottom: var(--w-space-5);
}

.workspace-stat {
  min-height: 78px;
  padding: var(--w-space-4);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-secondary);
  color: var(--w-text);
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  text-align: left;
}

.workspace-stat :deep(svg) {
  color: var(--w-brand);
  flex-shrink: 0;
}

.workspace-stat span {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.workspace-stat strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--w-text-lg);
}

.workspace-stat small {
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
}

.workspace-stat--action {
  cursor: pointer;
  transition: all var(--w-transition-base);
}

.workspace-stat--action:not(:disabled):hover {
  border-color: var(--w-border-strong);
  background: var(--w-bg-hover);
}

.workspace-stat--action:disabled {
  opacity: 0.65;
}

.stat-arrow {
  margin-left: auto;
  color: var(--w-text-tertiary) !important;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--w-space-3);
  margin-bottom: var(--w-space-5);
}

.search-box,
.sort-box {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--w-border-default);
  background: var(--w-bg-secondary);
  border-radius: var(--w-radius-md);
  color: var(--w-text-tertiary);
}

.search-box {
  flex: 1;
  min-width: 220px;
  padding: 0 12px;
}

.search-box input {
  height: 40px;
  width: 100%;
  background: transparent;
  border: 0;
  outline: 0;
  color: var(--w-text);
}

.search-box input::placeholder {
  color: var(--w-text-tertiary);
}

.sort-box {
  padding: 0 8px;
}

.sort-select {
  width: 128px;
}

.loading-box,
.empty-workbench {
  border: 1px dashed var(--w-border-default);
  border-radius: var(--w-radius-lg);
  background: var(--w-bg-secondary);
  padding: 76px var(--w-space-5);
  text-align: center;
}

.empty-illustration {
  width: 76px;
  height: 76px;
  border-radius: var(--w-radius-lg);
  background: var(--w-brand-soft);
  color: var(--w-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.empty-workbench h3 {
  font-size: var(--w-text-xl);
  font-weight: 600;
  margin-bottom: 6px;
}

.empty-workbench p {
  color: var(--w-text-secondary);
  margin-bottom: 22px;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: var(--w-space-3);
  flex-wrap: wrap;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(286px, 1fr));
  gap: var(--w-space-4);
}

.project-card {
  position: relative;
  min-height: 210px;
  padding: var(--w-space-4);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-secondary);
  cursor: pointer;
  overflow: hidden;
  text-align: left;
  transition: transform var(--w-transition-base), border-color var(--w-transition-base), box-shadow var(--w-transition-base), background var(--w-transition-base);
}

.project-card:hover {
  transform: translateY(-1px);
  border-color: var(--w-border-strong);
  box-shadow: var(--w-shadow-md);
}

.project-card-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}

.project-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--w-space-3);
  margin-bottom: var(--w-space-3);
}

.project-title-block {
  min-width: 0;
}

.project-title {
  max-width: 220px;
  font-size: var(--w-text-lg);
  font-weight: 600;
  color: var(--w-text);
}

.project-genre {
  display: inline-flex;
  margin-top: 6px;
  padding: 2px 8px;
  border-radius: var(--w-radius-sm);
  background: var(--w-bg-tertiary);
  color: var(--w-text-secondary);
  font-size: var(--w-text-xs);
}

.project-card-more {
  width: 30px;
  height: 30px;
  border-radius: var(--w-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--w-text-tertiary);
  transition: all var(--w-transition-fast);
  flex-shrink: 0;
}

.project-card-more:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.project-desc {
  min-height: 66px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: var(--w-space-4);
}

.project-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--w-space-2);
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
  margin-bottom: var(--w-space-4);
}

.project-actions {
  display: flex;
  align-items: center;
  gap: var(--w-space-2);
}

.project-card--new {
  border-style: dashed;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--w-text-secondary);
}

.project-card--new strong {
  color: var(--w-text);
}

.project-card--new span {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

@media (max-width: 767px) {
  .home-page {
    padding-top: var(--w-space-4);
  }

  .home-toolbar,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions > * {
    flex: 1;
  }

  .workspace-strip {
    grid-template-columns: 1fr;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
