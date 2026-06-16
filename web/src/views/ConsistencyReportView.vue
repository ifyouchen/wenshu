<script setup lang="ts">
/**
 * 一致性审查报告页（P8-12）。
 * - 加载报告及条目列表
 * - 按问题类型分组展示（character/timeline/location/plot）
 * - 支持跳转到对应章节
 * - 支持将条目标记为 handled / ignored / open
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NLayout, NLayoutContent, NPageHeader, NCard, NGrid, NGi,
  NTag, NButton, NSpace, NText, NEmpty, NSpin, NStatistic,
  NTabs, NTabPane, NDropdown, useMessage,
} from 'naive-ui'
import { getConsistencyReport, updateItemStatus } from '@/api/consistency'
import type { ConsistencyReport, ConsistencyReportItem } from '@/api/consistency'

const route = useRoute()
const router = useRouter()
const message = useMessage()

/** 报告 ID（来自路由参数）。 */
const reportId = route.params.reportId as string
/** 作品 ID（报告加载后从 report.projectId 取得）。 */
const projectId = computed(() => report.value?.projectId ?? '')

const loading = ref(false)
const report = ref<ConsistencyReport | null>(null)

/** 报告条目按类型分组。 */
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

/** 加载报告。 */
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

/**
 * 更新条目状态（handled / ignored / open）。
 * @param item   审查条目
 * @param status 新状态
 */
async function handleUpdateStatus(item: ConsistencyReportItem, status: 'open' | 'handled' | 'ignored') {
  try {
    await updateItemStatus(item.id, status)
    item.status = status
    message.success(status === 'handled' ? '已标记为已处理' : status === 'ignored' ? '已标记为忽略' : '已重新打开')
  } catch {
    message.error('状态更新失败')
  }
}

/**
 * 跳转到作品编辑器（无法精确定位到章节，跳转到作品入口由用户手动导航）。
 * 后端 ConsistencyReportItem 仅存储 chapterHint 文本，不含 chapterId。
 */
function jumpToProject() {
  if (projectId.value) {
    router.push(`/projects/${projectId.value}/editor`)
  }
}

/** 类型标签映射。 */
const typeLabel: Record<string, { label: string; type: 'error' | 'warning' | 'info' | 'default' }> = {
  character: { label: '人物一致性', type: 'error' },
  timeline:  { label: '时间线',     type: 'warning' },
  location:  { label: '地点描述',   type: 'info' },
  plot:      { label: '情节逻辑',   type: 'warning' },
  other:     { label: '其他问题',   type: 'default' },
}

/** 状态下拉菜单选项。 */
const statusOptions = [
  { label: '标记为已处理', key: 'handled' },
  { label: '标记为忽略', key: 'ignored' },
  { label: '重新打开', key: 'open' },
]

onMounted(loadReport)
</script>

<template>
  <NLayout style="height: 100vh; overflow: hidden">
    <NLayoutContent style="padding: 24px 32px; overflow-y: auto">

      <!-- 页头 -->
      <NPageHeader
        title="一致性审查报告"
        :subtitle="report ? `共 ${report.totalItems} 条问题，待处理 ${report.openItems} 条` : ''"
        @back="router.back()"
      >
        <template #extra>
          <NSpace>
            <NButton v-if="projectId" size="small" @click="jumpToProject">打开编辑器</NButton>
            <NButton @click="loadReport" :loading="loading" size="small">刷新</NButton>
          </NSpace>
        </template>
      </NPageHeader>

      <!-- 加载中 -->
      <NSpin v-if="loading" size="large" style="display: block; margin: 60px auto" />

      <!-- 空报告 -->
      <NEmpty
        v-else-if="!report || !report.items.length"
        description="本次审查未发现问题 🎉"
        style="margin-top: 48px"
      />

      <!-- 统计卡片 -->
      <NGrid v-else :cols="4" :x-gap="16" style="margin: 20px 0 24px">
        <NGi v-for="(group, key) in groupedItems" :key="key">
          <NCard size="small">
            <NStatistic
              :label="typeLabel[key]?.label ?? key"
              :value="group.length"
            />
          </NCard>
        </NGi>
      </NGrid>

      <!-- 按类型分组 Tab 展示 -->
      <NTabs v-if="report" type="line" animated>
        <NTabPane
          v-for="(items, key) in groupedItems"
          :key="key"
          :name="key"
          :tab="`${typeLabel[key]?.label ?? key}（${items.length}）`"
        >
          <div v-for="item in items" :key="item.id" class="issue-card">
            <div class="issue-header">
              <NSpace align="center" :size="8">
                <NTag
                  :type="typeLabel[key]?.type ?? 'default'"
                  size="small"
                  :bordered="false"
                >
                  {{ typeLabel[key]?.label ?? key }}
                </NTag>
                <NTag
                  :type="item.status === 'handled' ? 'success' : item.status === 'ignored' ? 'default' : 'warning'"
                  size="small"
                  :bordered="false"
                >
                  {{ item.status === 'handled' ? '已处理' : item.status === 'ignored' ? '已忽略' : '待处理' }}
                </NTag>
                <!-- 涉及章节提示 -->
                <NTag v-if="item.chapterHint" size="small" type="info" :bordered="false">
                  📍 {{ item.chapterHint }}
                </NTag>
                <!-- 涉及角色 -->
                <NTag v-if="item.character" size="small" type="default" :bordered="false">
                  👤 {{ item.character }}
                </NTag>
              </NSpace>

              <!-- 操作菜单 -->
              <NDropdown
                trigger="click"
                :options="statusOptions"
                @select="(k) => handleUpdateStatus(item, k as 'open' | 'handled' | 'ignored')"
              >
                <NButton text size="small">⋯</NButton>
              </NDropdown>
            </div>

            <!-- 问题描述 -->
            <NText style="font-size: 14px; display: block; margin: 8px 0 4px">
              {{ item.description }}
            </NText>

            <!-- 修改建议 -->
            <NText
              v-if="item.suggestion"
              depth="3"
              style="font-size: 12px"
            >
              💡 建议：{{ item.suggestion }}
            </NText>
          </div>
        </NTabPane>
      </NTabs>

    </NLayoutContent>
  </NLayout>
</template>

<style scoped>
.issue-card {
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  background: #fff;
  transition: box-shadow 0.15s;
}
.issue-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.issue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
