<script setup lang="ts">
/**
 * 作品首页：作品卡片网格、创建弹窗、空状态、配额 Tooltip。
 */
import { computed, ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst } from 'naive-ui'
import {
  NButton, NModal, NForm, NFormItem, NInput, NSelect, NDropdown,
  NEllipsis, NSpin, useMessage, useDialog, NIcon,
} from 'naive-ui'
import { Plus, MoreHorizontal, FileText, PenLine, Clock, Sparkles, BookOpen } from 'lucide-vue-next'
import { listProjects, createProject, deleteProject } from '@/api/project'
import QuotaTooltip from '@/components/QuotaTooltip.vue'
import OnboardingHint from '@/components/OnboardingHint.vue'
import { useOnboarding } from '@/composables/useOnboarding'
import type { ProjectInfo } from '@/api/types'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const ob = useOnboarding()
const showWelcomeHint = ref(false)

const projects = ref<ProjectInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInst | null>(null)

const createForm = reactive({ title: '', genre: '', synopsis: '' })

const totalWords = computed(() => projects.value.reduce((sum, project) => sum + (project.totalWords || 0), 0))
const latestProject = computed(() => projects.value[0])

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
  '玄幻': '#7C3AED',
  '仙侠': '#7C3AED',
  '都市': '#2563EB',
  '言情': '#E11D48',
  '历史': '#92400E',
  '科幻': '#0891B2',
  '悬疑': '#4B5563',
  '武侠': '#166534',
  '短剧': '#DB2777',
  '其他': '#5a6e8a',
}

const createRules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur', min: 1, max: 100 }],
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await listProjects()
    projects.value = res.data.data
    if (projects.value.length === 0 && ob.shouldShow('first-home')) {
      showWelcomeHint.value = true
    }
  } catch {
    message.error('加载作品列表失败')
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

function openProject(projectId: string) {
  router.push(`/projects/${projectId}/editor`)
}

function handleCardAction(key: string, project: ProjectInfo) {
  if (key === 'edit') {
    openProject(project.id)
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
      <!-- 页面标题区 -->
      <section class="home-hero">
        <div class="hero-copy">
          <p class="hero-label">作品总览</p>
          <h1>我的作品</h1>
          <p class="hero-desc">管理长篇项目、继续最近章节，并查看当前创作资产。</p>
        </div>
        <div class="hero-actions">
          <QuotaTooltip />
          <NButton type="primary" class="create-btn" @click="showCreate = true">
            <template #icon>
              <NIcon :component="Plus" :size="16" />
            </template>
            新建作品
          </NButton>
        </div>
      </section>

      <!-- 创作数据概览 -->
      <section class="stats-strip">
        <div class="stat-item">
          <NIcon :component="BookOpen" :size="18" class="stat-icon" />
          <div class="stat-info">
            <span class="stat-value">{{ projects.length }}</span>
            <span class="stat-label">作品</span>
          </div>
        </div>
        <div class="stat-item">
          <NIcon :component="PenLine" :size="18" class="stat-icon" />
          <div class="stat-info">
            <span class="stat-value">{{ formatWords(totalWords) }}</span>
            <span class="stat-label">总字数</span>
          </div>
        </div>
        <button
          class="stat-item stat-item--action"
          type="button"
          :disabled="!latestProject"
          @click="latestProject && openProject(latestProject.id)"
        >
          <NIcon :component="Clock" :size="18" class="stat-icon" />
          <div class="stat-info">
            <span class="stat-value">{{ latestProject?.title || '暂无' }}</span>
            <span class="stat-label">最近作品</span>
          </div>
        </button>
      </section>

      <!-- 首次访问欢迎引导 -->
      <OnboardingHint
        v-if="showWelcomeHint"
        icon="sparkles"
        title="欢迎来到文枢"
        description="文枢是专为长篇创作者打造的 AI 写作工作台。点击「新建作品」开始你的第一部小说，或者使用 Ctrl+K（Cmd+K）呼出命令面板快速导航。"
        action-label="开始创作"
        variant="welcome"
        @close="showWelcomeHint = false; ob.markDone('first-home')"
        @action="showWelcomeHint = false; ob.markDone('first-home'); showCreate = true"
      />

      <!-- 加载中 -->
      <div v-if="loading" class="loading-box">
        <NSpin size="large" />
      </div>

      <!-- 空状态 -->
      <section v-else-if="!projects.length" class="empty-workbench">
        <div class="empty-illustration">
          <NIcon :component="FileText" :size="48" />
        </div>
        <h3 class="empty-title">还没有作品</h3>
        <p class="empty-desc">开始你的创作之旅，第一部作品从这里诞生。</p>
        <NButton type="primary" class="empty-btn" @click="showCreate = true">
          <template #icon>
            <NIcon :component="Plus" :size="16" />
          </template>
          创建第一部作品
        </NButton>
      </section>

      <!-- 作品卡片网格 -->
      <section v-else class="project-grid">
        <div
          v-for="project in projects"
          :key="project.id"
          class="project-card"
          @click="openProject(project.id)"
        >
          <div
            class="project-card-accent"
            :style="{ background: genreColors[project.genre || '其他'] || genreColors['其他'] }"
          />
          <div class="project-card-content">
            <div class="project-card-header">
              <NEllipsis style="max-width: 220px; font-weight: 600">
                {{ project.title }}
              </NEllipsis>
              <NDropdown
                trigger="click"
                :options="[
                  { label: '打开', key: 'edit' },
                  { label: '删除', key: 'delete', props: { style: 'color: var(--w-danger)' } },
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
              {{ project.synopsis || '暂无简介' }}
            </p>

            <div class="project-card-footer">
              <div class="project-tags">
                <span v-if="project.genre" class="project-tag" :style="{ color: genreColors[project.genre] }">
                  {{ project.genre }}
                </span>
                <span class="project-tag project-tag--muted">{{ formatWords(project.totalWords) }}</span>
              </div>
              <span class="project-date">{{ formatDate(project.updatedAt) }}</span>
            </div>
          </div>
        </div>

        <!-- 新建作品占位卡片 -->
        <button class="project-card project-card--new" @click="showCreate = true">
          <div class="project-card-new-content">
            <div class="project-card-new-icon">
              <NIcon :component="Sparkles" :size="24" />
            </div>
            <span class="project-card-new-title">新建作品</span>
            <span class="project-card-new-desc">开启新的创作项目</span>
          </div>
        </button>
      </section>
    </div>

    <!-- 新建作品弹窗 -->
    <NModal v-model:show="showCreate" preset="card" title="新建作品" style="width: min(480px, 96vw)">
      <NForm ref="createFormRef" :model="createForm" :rules="createRules">
        <NFormItem label="作品名称 *" path="title">
          <NInput v-model:value="createForm.title" placeholder="请输入作品名称" />
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
            placeholder="一句话介绍你的故事（可选）"
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
  min-height: 100vh;
  padding-top: var(--w-space-6);
  padding-bottom: calc(var(--w-space-8) + 56px);
  overflow-y: auto;
}

.home-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: var(--w-space-6);
  flex-wrap: wrap;
  gap: var(--w-space-4);
}

.hero-copy {
  min-width: 0;
}

.hero-label {
  font-size: var(--w-text-sm);
  color: var(--w-brand);
  font-weight: 500;
  margin-bottom: 6px;
  letter-spacing: 0.04em;
}

.hero-copy h1 {
  font-family: var(--w-font-serif);
  font-size: var(--w-text-3xl);
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}

.hero-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  max-width: 480px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  flex-shrink: 0;
}

.create-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 18px;
}

/* 数据概览 */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--w-space-3);
  margin-bottom: var(--w-space-6);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--w-space-3);
  padding: var(--w-space-4);
  background: var(--w-bg-secondary);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  text-align: left;
  transition: all var(--w-transition-base);
}

.stat-item--action {
  cursor: pointer;
}

.stat-item--action:not(:disabled):hover {
  background: var(--w-bg-hover);
  border-color: var(--w-border-strong);
}

.stat-item--action:disabled {
  opacity: 0.6;
  cursor: default;
}

.stat-icon {
  color: var(--w-brand);
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-value {
  font-size: var(--w-text-lg);
  font-weight: 600;
  color: var(--w-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-label {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  margin-top: 2px;
}

/* 加载与空状态 */
.loading-box,
.empty-workbench {
  border: 1px dashed var(--w-border-default);
  border-radius: var(--w-radius-lg);
  background: var(--w-bg-secondary);
  padding: 80px var(--w-space-5);
  text-align: center;
}

.empty-illustration {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--w-brand-soft);
  color: var(--w-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.empty-title {
  font-size: var(--w-text-xl);
  font-weight: 600;
  margin-bottom: 6px;
}

.empty-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
  margin-bottom: 24px;
}

.empty-btn {
  height: 40px;
  padding: 0 20px;
}

/* 作品卡片网格 */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--w-space-4);
}

.project-card {
  position: relative;
  background: var(--w-bg-secondary);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--w-transition-base);
  text-align: left;
}

.project-card:hover {
  transform: translateY(-2px);
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

.project-card-content {
  padding: var(--w-space-4);
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--w-space-2);
  margin-bottom: var(--w-space-3);
  font-size: var(--w-text-base);
  color: var(--w-text);
}

.project-card-more {
  width: 28px;
  height: 28px;
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
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
  margin-bottom: var(--w-space-4);
}

.project-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--w-space-2);
}

.project-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.project-tag {
  font-size: var(--w-text-xs);
  font-weight: 500;
  padding: 3px 8px;
  border-radius: var(--w-radius-sm);
  background: var(--w-bg-tertiary);
}

.project-tag--muted {
  color: var(--w-text-secondary);
}

.project-date {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  flex-shrink: 0;
}

/* 新建作品占位卡片 */
.project-card--new {
  border-style: dashed;
  background: transparent;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-card--new:hover {
  background: var(--w-bg-hover);
  border-color: var(--w-brand);
}

.project-card-new-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.project-card-new-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--w-brand-soft);
  color: var(--w-brand);
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-card-new-title {
  font-size: var(--w-text-base);
  font-weight: 600;
  color: var(--w-text);
}

.project-card-new-desc {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

@media (max-width: 767px) {
  .home-page {
    padding-top: var(--w-space-4);
  }

  .home-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .stats-strip {
    grid-template-columns: 1fr;
  }

  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
