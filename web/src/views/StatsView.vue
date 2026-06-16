<script setup lang="ts">
/**
 * 写作统计看板（P8-10）。
 * - 今日字数与目标进度
 * - 7 日写作趋势折线图（ECharts 轻量替代：内联 canvas + CSS）
 * - 365 天热力图（CSS Grid 渲染，仿 GitHub Contribution Graph）
 * - 各作品进度列表
 * - 月度摘要（切换月份查询）
 */
// 使用 h 函数需要从 vue 导入
import {computed, h, onMounted, ref} from 'vue'
import type {DataTableColumns} from 'naive-ui'
import {
  NCard,
  NDataTable,
  NEmpty,
  NGrid,
  NGridItem,
  NLayout,
  NLayoutContent,
  NPageHeader,
  NProgress,
  NSelect,
  NSpace,
  NSpin,
  NStatistic,
  NTag,
  NText,
} from 'naive-ui'
import type {DailyStats, HeatmapData, MonthlySummary, ProjectProgress, WritingOverview} from '@/api/stats'
import {getMonthlySummary, getProjectProgress, getWritingHeatmap, getWritingOverview,} from '@/api/stats'
import {useRouter} from 'vue-router'

const router = useRouter()

// --- 数据状态 ---
const overviewLoading = ref(false)
const heatmapLoading = ref(false)
const projectLoading = ref(false)
const monthlyLoading = ref(false)

const overview = ref<WritingOverview | null>(null)
const heatmap = ref<HeatmapData | null>(null)
const projects = ref<ProjectProgress[]>([])
const monthly = ref<MonthlySummary | null>(null)

/** 当前选择月份（YYYY-MM）。 */
const selectedMonth = ref<string>(new Date().toISOString().slice(0, 7))

/** 月份选项（最近 12 个月）。 */
const monthOptions = computed(() => {
  const opts: { label: string; value: string }[] = []
  const now = new Date()
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const val = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    opts.push({ label: `${d.getFullYear()}年${d.getMonth() + 1}月`, value: val })
  }
  return opts
})

// --- 热力图渲染 ---
/** 将 HeatmapData 转换为 Map<date, chars>，便于快速查找。 */
const heatmapMap = computed<Map<string, number>>(() => {
  const map = new Map<string, number>()
  heatmap.value?.days.forEach(d => map.set(d.date, d.chars))
  return map
})

/** 生成热力图 52 周 × 7 天的日期格。返回二维数组（外：周，内：天）。 */
const heatmapWeeks = computed<Array<Array<{ date: string; chars: number; level: number }>>>(() => {
  const weeks: Array<Array<{ date: string; chars: number; level: number }>> = []
  const today = new Date()
  // 从今天往前推 364 天，保证 365 格
  const start = new Date(today)
  start.setDate(start.getDate() - 364)
  // 补全到最近周日（使周一为起始）
  const dayOfWeek = (start.getDay() + 6) % 7 // 0=Mon...6=Sun
  start.setDate(start.getDate() - dayOfWeek)

  let cur = new Date(start)
  while (cur <= today) {
    const week: Array<{ date: string; chars: number; level: number }> = []
    for (let d = 0; d < 7; d++) {
      const dateStr = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`
      const chars = heatmapMap.value.get(dateStr) ?? 0
      week.push({ date: dateStr, chars, level: charsToLevel(chars) })
      cur.setDate(cur.getDate() + 1)
    }
    weeks.push(week)
  }
  return weeks
})

/** 字数转热力图级别（0-4）。 */
function charsToLevel(chars: number): number {
  if (chars <= 0) return 0
  if (chars < 500) return 1
  if (chars < 1500) return 2
  if (chars < 3000) return 3
  return 4
}

const levelColors = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']

// --- 趋势图（7 日，内联 SVG）---
/** 生成 7 日趋势的 SVG polyline 路径。 */
const trendSvgPath = computed(() => {
  const trend: DailyStats[] = overview.value?.trend ?? []
  if (!trend.length) return ''
  const max = Math.max(...trend.map(d => d.chars), 1)
  const W = 300
  const H = 80
  const pts = trend.map((d, i) => {
    const x = (i / (trend.length - 1)) * W
    const y = H - (d.chars / max) * (H - 8) - 4
    return `${x.toFixed(1)},${y.toFixed(1)}`
  })
  return pts.join(' ')
})

// --- 作品进度表格 ---
/** 作品进度 DataTable 列定义。 */
const projectColumns = computed<DataTableColumns<ProjectProgress>>(() => [
  {
    title: '作品',
    key: 'title',
    render(row) {
      return row.title
    },
  },
  {
    title: '总字数',
    key: 'totalWords',
    width: 90,
    render(row) {
      return `${row.totalWords.toLocaleString()} 字`
    },
  },
  {
    title: '今日字数',
    key: 'todayChars',
    width: 90,
    render(row) {
      return `${row.todayChars.toLocaleString()} 字`
    },
  },
  {
    title: '今日进度',
    key: 'progress',
    width: 140,
    render(row) {
      const pct = Math.min(Math.round(row.progress * 100), 100)
      return h(
        NProgress,
        { type: 'line', percentage: pct, processingColor: '#18a058', showIndicator: false, height: 6, style: 'margin-top: 4px' },
      )
    },
  },
])

// --- 数据加载 ---
async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await getWritingOverview()
    overview.value = res.data.data
  } catch { /* 静默 */ }
  finally { overviewLoading.value = false }
}

async function loadHeatmap() {
  heatmapLoading.value = true
  try {
    const res = await getWritingHeatmap()
    heatmap.value = res.data.data
  } catch { /* 静默 */ }
  finally { heatmapLoading.value = false }
}

async function loadProjectProgress() {
  projectLoading.value = true
  try {
    const res = await getProjectProgress()
    projects.value = res.data.data
  } catch { /* 静默 */ }
  finally { projectLoading.value = false }
}

async function loadMonthly(ym: string) {
  monthlyLoading.value = true
  try {
    const res = await getMonthlySummary(ym)
    monthly.value = res.data.data
  } catch { /* 静默 */ }
  finally { monthlyLoading.value = false }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadHeatmap(), loadProjectProgress()])
  await loadMonthly(selectedMonth.value)
})

async function onMonthChange(val: string) {
  selectedMonth.value = val
  await loadMonthly(val)
}
</script>

<template>
  <NLayout style="height: 100vh; background: #f8f8f8">
    <!-- P8-14：移动端缩减 padding 并为底部导航预留空间 -->
    <NLayoutContent class="stats-content">

      <!-- 页头 -->
      <NPageHeader title="写作统计" subtitle="记录每一次坚持" @back="router.back()">
        <template #extra>
          <NTag type="success" v-if="overview?.streak">
            连续 {{ overview.streak }} 天
          </NTag>
        </template>
      </NPageHeader>

      <!-- 今日概览 -->
      <NCard title="今日" style="margin-top: 16px">
        <NSpin :show="overviewLoading">
          <NGrid :cols="4" :x-gap="16" :y-gap="16">
            <NGridItem>
              <NStatistic label="今日字数">
                <template #default>
                  <span style="font-size: 28px; font-weight: 700; color: #18a058">
                    {{ overview?.todayChars?.toLocaleString() ?? 0 }}
                  </span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">字</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="每日目标">
                <template #default>
                  <span style="font-size: 24px">{{ overview?.dailyGoal?.toLocaleString() ?? 0 }}</span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">字</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="今日进度">
                <template #default>
                  <NProgress
                    type="circle"
                    :percentage="Math.min(Math.round((overview?.todayProgress ?? 0) * 100), 100)"
                    :color="'#18a058'"
                    style="width: 64px"
                  />
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="累计字数">
                <template #default>
                  <span style="font-size: 24px">{{ overview?.totalChars?.toLocaleString() ?? 0 }}</span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">字</span>
                </template>
              </NStatistic>
            </NGridItem>
          </NGrid>

          <!-- 7 日趋势折线 -->
          <div v-if="overview?.trend?.length" style="margin-top: 20px">
            <NText depth="3" style="font-size: 12px; display: block; margin-bottom: 6px">近 7 日趋势</NText>
            <svg width="100%" height="88" viewBox="0 0 300 88" preserveAspectRatio="none">
              <defs>
                <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#18a058" stop-opacity="0.25" />
                  <stop offset="100%" stop-color="#18a058" stop-opacity="0.02" />
                </linearGradient>
              </defs>
              <!-- 渐变填充面积 -->
              <path
                v-if="trendSvgPath"
                :d="`M${trendSvgPath.split(' ')[0]} L${trendSvgPath} L300,84 L0,84 Z`"
                fill="url(#trendGrad)"
              />
              <!-- 折线 -->
              <polyline
                v-if="trendSvgPath"
                :points="trendSvgPath"
                fill="none"
                stroke="#18a058"
                stroke-width="2"
                stroke-linejoin="round"
              />
              <!-- 数据点 -->
              <template v-for="(d, i) in (overview?.trend ?? [])" :key="d.date">
                <circle
                  v-if="overview?.trend"
                  :cx="(i / (overview.trend.length - 1)) * 300"
                  :cy="88 - (d.chars / Math.max(...overview.trend.map(x => x.chars), 1)) * 76 - 4"
                  r="3"
                  fill="#18a058"
                />
              </template>
            </svg>
            <!-- 日期标签 -->
            <div style="display: flex; justify-content: space-between; margin-top: 2px">
              <NText v-for="d in overview?.trend" :key="d.date" depth="3" style="font-size: 10px">
                {{ d.date.slice(5) }}
              </NText>
            </div>
          </div>
        </NSpin>
      </NCard>

      <!-- 热力图 -->
      <NCard title="365 天热力图" style="margin-top: 16px">
        <NSpin :show="heatmapLoading">
          <div v-if="heatmapWeeks.length" style="overflow-x: auto">
            <!-- 星期标签 -->
            <div style="display: flex; gap: 3px; margin-bottom: 4px; padding-left: 0">
              <div
                v-for="week in heatmapWeeks"
                :key="week[0]?.date"
                style="display: flex; flex-direction: column; gap: 3px"
              >
                <div
                  v-for="day in week"
                  :key="day.date"
                  :title="`${day.date}：${day.chars} 字`"
                  :style="{
                    width: '12px',
                    height: '12px',
                    borderRadius: '2px',
                    background: levelColors[day.level],
                    cursor: 'default',
                    flexShrink: 0,
                  }"
                />
              </div>
            </div>
            <!-- 颜色图例 -->
            <NSpace :size="6" align="center" style="margin-top: 8px">
              <NText depth="3" style="font-size: 11px">少</NText>
              <div v-for="(color, i) in levelColors" :key="i"
                   :style="{ width: '12px', height: '12px', borderRadius: '2px', background: color }" />
              <NText depth="3" style="font-size: 11px">多</NText>
            </NSpace>
          </div>
          <NEmpty v-else description="暂无热力图数据" />
        </NSpin>
      </NCard>

      <!-- 各作品进度 -->
      <NCard title="作品进度" style="margin-top: 16px">
        <NSpin :show="projectLoading">
          <NDataTable
            v-if="projects.length"
            :columns="projectColumns"
            :data="projects"
            :bordered="false"
            size="small"
          />
          <NEmpty v-else description="暂无作品数据" />
        </NSpin>
      </NCard>

      <!-- 月度摘要 -->
      <NCard style="margin-top: 16px">
        <template #header>
          <NSpace align="center">
            <span>月度摘要</span>
            <NSelect
              :value="selectedMonth"
              :options="monthOptions"
              size="small"
              style="width: 140px"
              @update:value="onMonthChange"
            />
          </NSpace>
        </template>

        <NSpin :show="monthlyLoading">
          <NGrid v-if="monthly" :cols="3" :x-gap="24" style="margin-bottom: 16px">
            <NGridItem>
              <NStatistic label="月总字数">
                <template #default>
                  <span style="font-size: 24px">{{ monthly.totalChars?.toLocaleString() ?? 0 }}</span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">字</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="活跃天数">
                <template #default>
                  <span style="font-size: 24px">{{ monthly.activeDays ?? 0 }}</span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">天</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="日均字数">
                <template #default>
                  <span style="font-size: 24px">{{ Math.round(monthly.avgCharsPerDay ?? 0).toLocaleString() }}</span>
                  <span style="font-size: 13px; color: #999; margin-left: 4px">字/天</span>
                </template>
              </NStatistic>
            </NGridItem>
          </NGrid>
          <NEmpty v-else description="暂无月度数据" />
        </NSpin>
      </NCard>

    </NLayoutContent>
  </NLayout>
</template>


<style scoped>
/* ─── P8-14 写作统计页移动端适配 ─── */
.stats-content {
  padding: 16px;
  overflow-y: auto;
  max-width: 1100px;
  margin: 0 auto;
  /* 移动端底部导航 56px + 安全区域 */
  padding-bottom: 72px;
}

@media (min-width: 768px) {
  .stats-content {
    padding: 24px 32px;
    padding-bottom: 24px;
  }
}
</style>
