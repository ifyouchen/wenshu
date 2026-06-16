<script setup lang="ts">
/**
 * 作品首页（P8-05）：作品卡片网格、创建弹窗、空状态、配额 Tooltip。
 */
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst } from 'naive-ui'
import {
  NEmpty, NButton, NH1, NSpace, NGrid, NGi, NCard, NText,
  NModal, NForm, NFormItem, NInput, NSelect, NTag, NSpin,
  NDropdown, NEllipsis, useMessage, useDialog
} from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { listProjects, createProject, deleteProject } from '@/api/project'
import QuotaTooltip from '@/components/QuotaTooltip.vue'
import type { ProjectInfo } from '@/api/types'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const dialog = useDialog()

const projects = ref<ProjectInfo[]>([])
const loading = ref(false)
const showCreate = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInst | null>(null)

const createForm = reactive({ title: '', genre: '', synopsis: '' })

const genreOptions = [
  { label: '玄幻', value: '玄幻' }, { label: '仙侠', value: '仙侠' },
  { label: '都市', value: '都市' }, { label: '言情', value: '言情' },
  { label: '历史', value: '历史' }, { label: '科幻', value: '科幻' },
  { label: '悬疑', value: '悬疑' }, { label: '武侠', value: '武侠' },
  { label: '短剧', value: '短剧' }, { label: '其他', value: '其他' },
]

const createRules = {
  title: [{ required: true, message: '请输入作品名称', trigger: 'blur', min: 1, max: 100 }],
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await listProjects()
    projects.value = res.data.data
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
</script>

<template>
  <div class="home-page">
    <!-- 顶部操作栏 -->
    <div class="home-header">
      <NH1 style="margin: 0">我的作品</NH1>
      <NSpace align="center">
        <QuotaTooltip />
        <NButton type="primary" @click="showCreate = true">+ 新建作品</NButton>
        <NButton text @click="auth.logoutAction().then(() => router.push('/login'))">退出</NButton>
      </NSpace>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" style="text-align: center; padding: 60px">
      <NSpin size="large" />
    </div>

    <!-- 空状态 -->
    <NEmpty
      v-else-if="!projects.length"
      description="还没有作品，开始你的创作之旅吧"
      style="padding: 80px 0"
    >
      <template #extra>
        <NButton type="primary" @click="showCreate = true">创建第一部作品</NButton>
      </template>
    </NEmpty>

    <!-- 作品卡片网格 -->
    <NGrid v-else :cols="3" :x-gap="16" :y-gap="16" responsive="screen" :item-responsive="true">
      <NGi v-for="project in projects" :key="project.id" span="3 m:1">
        <NCard
          hoverable
          class="project-card"
          @click="openProject(project.id)"
        >
          <template #header>
            <NEllipsis style="max-width: 220px; font-weight: 600">{{ project.title }}</NEllipsis>
          </template>
          <template #header-extra>
            <NDropdown
              trigger="click"
              :options="[
                { label: '打开', key: 'edit' },
                { label: '删除', key: 'delete', props: { style: 'color: #d03050' } },
              ]"
              @select="(k) => handleCardAction(k, project)"
              @click.stop
            >
              <NButton text size="tiny" @click.stop>⋯</NButton>
            </NDropdown>
          </template>

          <NText depth="3" style="font-size: 13px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 38px">
            {{ project.synopsis || '暂无简介' }}
          </NText>

          <template #footer>
            <NSpace justify="space-between" align="center">
              <NSpace :size="6">
                <NTag v-if="project.genre" size="small" :bordered="false" type="info">{{ project.genre }}</NTag>
                <NTag size="small" :bordered="false">{{ formatWords(project.totalWords) }}</NTag>
              </NSpace>
              <NText depth="3" style="font-size: 12px">
                {{ new Date(project.updatedAt).toLocaleDateString() }}
              </NText>
            </NSpace>
          </template>
        </NCard>
      </NGi>
    </NGrid>

    <!-- 新建作品弹窗 -->
    <NModal v-model:show="showCreate" preset="card" title="新建作品" style="width: 480px">
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
.home-page { padding: 32px; max-width: 1200px; margin: 0 auto; }
.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}
.project-card { cursor: pointer; transition: transform 0.1s; }
.project-card:hover { transform: translateY(-2px); }
</style>
