<script setup lang="ts">
/**
 * 一致性审查报告页（P8-12）。
 * - 加载报告及条目列表
 * - 按问题类型分组展示（character/timeline/location/plot）
 * - 支持跳转到对应章节
 * - 支持将条目标记为 handled / ignored / open
 */
import { ref, onMounted, computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NLayout, NLayoutContent, NPageHeader, NCard, NGrid, NGi,
  NTag, NButton, NSpace, NText, NEmpty, NSpin, NStatistic,
  NTabs, NTabPane, NDropdown, NIcon, useMessage,
} from 'naive-ui'
import {
  MapPin,
  User,
  AlertCircle,
  Clock,
  Home,
  Bookmark,
  AlertTriangle,
  CheckCircle2,
  HelpCircle,
} from 'lucide-vue-next'
import { getConsistencyReport, updateItemStatus } from '@/api/consistency'
import type { ConsistencyReport, ConsistencyReportItem } from '@/api/consistency'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const reportId = route.params.reportId as string
const projectId = computed(() => report.value?.projectId ?? '')

const loading = ref(false)
const report = ref<ConsistencyReport | null>(null)

const groupedItems = computed(() => {
  if (!report.value) return {}
  const groups: Record<string, ConsistencyReportItem[]> = {
    character: [],
    timeline: [],
    location: [],
    plot: [],
    other: [],
  }
  for (const item of report.value.items) {
    const key = item.type && groups[item.type] ? item.type : 'other'
    groups[key].push(item)
  }
  return groups
})

async function loadReport() {
  loading.value = true
  try {
    const res = await getConsistencyReport(reportId)
    report.value = res.data.data
  } catch {
    message.error('报告加载失败，请检查报告 ID 是否有效')
  } finally {
    loading.value = false
  }
}

async function handleUpdateStatus(item: ConsistencyReportItem, status: 'open' | 'handled' | 'ignored') {
  try {
    await updateItemStatus(item.id, status)
    item.status = status
    message.success(status === 'handled' ? '已标记为已处理' : status === 'ignored' ? '已标记为忽略' : '已重新打开')
  } catch {
    message.error('状态更新失败')
  }
}

function jumpToProject() {
  if (projectId.value) {
    router.push(`/projects/${projectId.value}/editor`)
  }
}

const typeMeta: Record<string, { label: string; type: 'error' | 'warning' | 'info' | 'default'; icon: any }> = {
  character: { label: '人物一致性', type: 'error', icon: User },
  timeline:  { label: '时间线',     type: 'warning', icon: Clock },
  location:  { label: '地点描述',   type: 'info', icon: Home },
  plot:      { label: '情节逻辑',   type: 'warning', icon: AlertTriangle },
  other:     { label: '其他问题',   type: 'default', icon: HelpCircle },
}

const statusOptions = [
  { label: '标记为已处理', key: 'handled' },
  { label: '标记为忽略', key: 'ignored' },
  { label: '重新打开', key: 'open' },
]

const statusIcon = {
  handled: CheckCircle2,
  ignored: AlertCircle,
  open: AlertTriangle,
}

function typeTabLabel(key: string, count: number) {
  const meta = typeMeta[key]
  return () => h('span', { style: { display: 'inline-flex', alignItems: 'center', gap: '5px' } }, [
    h(NIcon, { component: meta?.icon ?? HelpCircle, size: 13 }),
    `${meta?.label ?? key}（${count}）`,
  ])
}

onMounted(loadReport)
</script>

<template>
  <NLayout class="report-layout">
    <NLayoutContent class="report-content">
      <NPageHeader
        title="一致性审查报告"
        :subtitle="report ? `共 ${report.totalItems} 条问题，待处理 ${report.openItems} 条` : ''"
        @back="router.back()"
      >
        <template #extra>
          <NSpace>
            <NButton v-if="projectId" size="small" @click="jumpToProject">打开编辑器</NButton>
            <NButton :loading="loading" size="small" @click="loadReport">刷新</NButton>
          </NSpace>
        </template>
      </NPageHeader>

      <NSpin v-if="loading" size="large" class="report-spin" />

      <NEmpty
        v-else-if="!report || !report.items.length"
        description="本次审查未发现问题"
        class="report-empty"
      />

      <template v-else>
        <NGrid :cols="4" :x-gap="16" class="report-stats">
          <NGi v-for="(group, key) in groupedItems" :key="key">
            <NCard size="small">
              <NStatistic
                :label="typeMeta[key]?.label ?? key"
                :value="group.length"
              />
            </NCard>
          </NGi>
        </NGrid>

        <NTabs type="line" animated>
          <NTabPane
            v-for="(items, key) in groupedItems"
            :key="key"
            :name="key"
            :tab="typeTabLabel(key, items.length)"
          >
            <div v-for="item in items" :key="item.id" class="issue-card">
              <div class="issue-header">
                <NSpace align="center" :size="8">
                  <NTag
                    :type="typeMeta[key]?.type ?? 'default'"
                    size="small"
                    :bordered="false"
                  >
                    <template #icon>
                      <NIcon :component="typeMeta[key]?.icon ?? HelpCircle" :size="12" />
                    </template>
                    {{ typeMeta[key]?.label ?? key }}
                  </NTag>
                  <NTag
                    :type="item.status === 'handled' ? 'success' : item.status === 'ignored' ? 'default' : 'warning'"
                    size="small"
                    :bordered="false"
                  >
                    <template #icon>
                      <NIcon :component="statusIcon[item.status]" :size="12" />
                    </template>
                    {{ item.status === 'handled' ? '已处理' : item.status === 'ignored' ? '已忽略' : '待处理' }}
                  </NTag>
                  <NTag v-if="item.chapterHint" size="small" type="info" :bordered="false">
                    <template #icon>
                      <NIcon :component="MapPin" :size="12" />
                    </template>
                    {{ item.chapterHint }}
                  </NTag>
                  <NTag v-if="item.character" size="small" type="default" :bordered="false">
                    <template #icon>
                      <NIcon :component="User" :size="12" />
                    </template>
                    {{ item.character }}
                  </NTag>
                </NSpace>

                <NDropdown
                  trigger="click"
                  :options="statusOptions"
                  @select="(k) => handleUpdateStatus(item, k as 'open' | 'handled' | 'ignored')"
                >
                  <NButton text size="small">更多</NButton>
                </NDropdown>
              </div>

              <NText class="issue-desc">
                {{ item.description }}
              </NText>

              <NText
                v-if="item.suggestion"
                depth="3"
                class="issue-suggestion"
              >
                <NIcon :component="Bookmark" :size="12" />
                建议：{{ item.suggestion }}
              </NText>
            </div>
          </NTabPane>
        </NTabs>
      </template>
    </NLayoutContent>
  </NLayout>
</template>

<style scoped>
.report-layout {
  height: calc(100vh - var(--w-topbar-height));
  overflow: hidden;
  background: var(--w-bg);
}

.report-content {
  padding: var(--w-space-4) var(--w-space-5);
  overflow-y: auto;
  max-width: var(--w-max-content-width);
  margin: 0 auto;
}

.report-spin {
  display: block;
  margin: 60px auto;
}

.report-empty {
  margin-top: 48px;
}

.report-stats {
  margin: var(--w-space-4) 0 var(--w-space-5);
}

.issue-card {
  padding: var(--w-space-3) var(--w-space-4);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  margin-bottom: var(--w-space-3);
  background: var(--w-bg-secondary);
  transition: box-shadow var(--w-transition-base);
}

.issue-card:hover {
  box-shadow: var(--w-shadow-sm);
}

.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.issue-desc {
  font-size: var(--w-text-sm);
  display: block;
  margin: var(--w-space-2) 0 var(--w-space-1);
}

.issue-suggestion {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
</style>
