<script setup lang="ts">
/**
 * 写作统计看板。
 */
import { computed, h, onMounted, ref } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton, NCard, NDataTable, NEmpty, NGrid, NGridItem, NProgress,
  NRadioButton, NRadioGroup, NSelect, NSpin, NStatistic, NTag, NText, NIcon,
} from 'naive-ui'
import {
  PenLine,
  Calendar,
  Flame,
  TrendingUp,
  Clock,
  BookOpen,
  Download,
} from 'lucide-vue-next'
import type {
  DailyStats, HeatmapData, MonthlySummary, ProjectProgress, TimeHeatmapEntry, WritingOverview,
} from '@/api/stats'
import {
  getMonthlySummary, getProjectProgress, getWritingHeatmap, getWritingOverview, getWritingTimeHeatmap,
} from '@/api/stats'
const overviewLoading = ref(false)
const heatmapLoading = ref(false)
const projectLoading = ref(false)
const monthlyLoading = ref(false)
const timeHeatmapLoading = ref(false)

const overview = ref<WritingOverview | null>(null)
const heatmap = ref<HeatmapData | null>(null)
const projects = ref<ProjectProgress[]>([])
const monthly = ref<MonthlySummary | null>(null)
const timeHeatmapData = ref<TimeHeatmapEntry[]>([])

const selectedMonth = ref<string>(new Date().toISOString().slice(0, 7))

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

const timeHeatmapMap = computed<Map<string, number>>(() => {
  const map = new Map<string, number>()
  timeHeatmapData.value.forEach(e => map.set(`${e.weekday}-${e.hour}`, e.totalChars))
  return map
})

const timeHeatmapMax = computed(() => {
  if (!timeHeatmapData.value.length) return 1
  return Math.max(...timeHeatmapData.value.map(e => e.totalChars), 1)
})

function timeCharsToLevel(chars: number): number {
  if (chars <= 0) return 0
  const max = timeHeatmapMax.value
  if (chars < max * 0.2) return 1
  if (chars < max * 0.4) return 2
  if (chars < max * 0.7) return 3
  return 4
}

const weekdayLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const trendDimension = ref<'total' | 'manual' | 'ai'>('total')

const heatmapMap = computed<Map<string, number>>(() => {
  const map = new Map<string, number>()
  heatmap.value?.days.forEach(d => map.set(d.date, d.chars))
  return map
})

const heatmapWeeks = computed<Array<Array<{ date: string; chars: number; level: number }>>>(() => {
  const weeks: Array<Array<{ date: string; chars: number; level: number }>> = []
  const today = new Date()
  const start = new Date(today)
  start.setDate(start.getDate() - 364)
  const dayOfWeek = (start.getDay() + 6) % 7
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

function charsToLevel(chars: number): number {
  if (chars <= 0) return 0
  if (chars < 500) return 1
  if (chars < 1500) return 2
  if (chars < 3000) return 3
  return 4
}

const levelColors = [
  'var(--w-bg-tertiary)',
  'rgba(91, 138, 114, 0.25)',
  'rgba(91, 138, 114, 0.45)',
  'rgba(91, 138, 114, 0.65)',
  'rgba(91, 138, 114, 0.9)',
]

function getTrendValue(d: DailyStats): number {
  if (trendDimension.value === 'manual') return d.manualChars ?? d.chars
  if (trendDimension.value === 'ai') return d.aiAcceptedChars ?? 0
  return d.chars
}

const trendSvgPath = computed(() => {
  const trend: DailyStats[] = overview.value?.trend ?? []
  if (!trend.length) return ''
  const max = Math.max(...trend.map(d => getTrendValue(d)), 1)
  const W = 300
  const H = 80
  const pts = trend.map((d, i) => {
    const x = (i / (trend.length - 1)) * W
    const y = H - (getTrendValue(d) / max) * (H - 8) - 4
    return `${x.toFixed(1)},${y.toFixed(1)}`
  })
  return pts.join(' ')
})

function downloadTrendChart() {
  const svgEl = document.querySelector('.trend-svg') as SVGElement | null
  if (!svgEl) return
  const xml = new XMLSerializer().serializeToString(svgEl)
  const blob = new Blob([xml], { type: 'image/svg+xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'writing-trend.svg'
  a.click()
  URL.revokeObjectURL(url)
}

const projectColumns = computed<DataTableColumns<ProjectProgress>>(() => [
  {
    title: '作品',
    key: 'title',
    render(row) {
      return h('span', { class: 'project-table-title' }, row.title)
    },
  },
  {
    title: '总字数',
    key: 'totalWords',
    width: 100,
    render(row) {
      return `${row.totalWords.toLocaleString()} 字`
    },
  },
  {
    title: '今日字数',
    key: 'todayChars',
    width: 100,
    render(row) {
      return `${row.todayChars.toLocaleString()} 字`
    },
  },
  {
    title: '今日进度',
    key: 'progress',
    width: 160,
    render(row) {
      const pct = Math.min(Math.round(row.progress * 100), 100)
      return h(
        NProgress,
        {
          type: 'line',
          percentage: pct,
          color: 'var(--w-brand)',
          showIndicator: false,
          height: 6,
          style: 'margin-top: 4px',
        },
      )
    },
  },
])

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

async function loadTimeHeatmap() {
  timeHeatmapLoading.value = true
  try {
    const res = await getWritingTimeHeatmap()
    timeHeatmapData.value = res.data.data
  } catch { /* 静默 */ }
  finally { timeHeatmapLoading.value = false }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadHeatmap(), loadProjectProgress(), loadTimeHeatmap()])
  await loadMonthly(selectedMonth.value)
})

async function onMonthChange(val: string) {
  selectedMonth.value = val
  await loadMonthly(val)
}

function formatNumber(n: number) {
  return n.toLocaleString()
}
</script>

<template>
  <div class="stats-page">
    <div class="w-container">
      <!-- 页头 -->
      <header class="stats-header">
        <div>
          <p class="stats-label">创作数据</p>
          <h1>写作统计</h1>
          <p class="stats-desc">记录每一次坚持，量化你的创作轨迹。</p>
        </div>
        <NTag v-if="overview?.streak" class="streak-tag" :bordered="false">
          <template #icon>
            <NIcon :component="Flame" :size="14" />
          </template>
          连续 {{ overview.streak }} 天
        </NTag>
      </header>

      <!-- 今日概览 -->
      <NCard class="stats-card" :bordered="false">
        <div class="card-header">
          <NIcon :component="PenLine" :size="18" class="card-header-icon" />
          <span class="card-header-title">今日概览</span>
        </div>
        <NSpin :show="overviewLoading">
          <div class="overview-grid">
            <div class="overview-item overview-item--highlight">
              <span class="overview-label">今日字数</span>
              <div class="overview-value-wrap">
                <span class="overview-value overview-value--large">
                  {{ formatNumber(overview?.todayChars ?? 0) }}
                </span>
                <span class="overview-unit">字</span>
              </div>
            </div>
            <div class="overview-item">
              <span class="overview-label">每日目标</span>
              <div class="overview-value-wrap">
                <span class="overview-value">{{ formatNumber(overview?.dailyGoal ?? 0) }}</span>
                <span class="overview-unit">字</span>
              </div>
            </div>
            <div class="overview-item">
              <span class="overview-label">今日进度</span>
              <NProgress
                type="circle"
                :percentage="Math.min(Math.round((overview?.todayProgress ?? 0) * 100), 100)"
                :color="'var(--w-brand)'"
                style="width: 64px"
              />
            </div>
            <div class="overview-item">
              <span class="overview-label">累计字数</span>
              <div class="overview-value-wrap">
                <span class="overview-value">{{ formatNumber(overview?.totalChars ?? 0) }}</span>
                <span class="overview-unit">字</span>
              </div>
            </div>
          </div>

          <!-- 7 日趋势 -->
          <div v-if="overview?.trend?.length" class="trend-section">
            <div class="trend-header">
              <div class="trend-title">
                <NIcon :component="TrendingUp" :size="16" />
                近 7 日趋势
              </div>
              <NRadioGroup v-model:value="trendDimension" size="small">
                <NRadioButton value="total">合计</NRadioButton>
                <NRadioButton value="manual">手动输入</NRadioButton>
                <NRadioButton value="ai">辅助生成</NRadioButton>
              </NRadioGroup>
            </div>
            <svg class="trend-svg" width="100%" height="100" viewBox="0 0 300 100" preserveAspectRatio="none">
              <defs>
                <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="var(--w-brand)" stop-opacity="0.2" />
                  <stop offset="100%" stop-color="var(--w-brand)" stop-opacity="0.02" />
                </linearGradient>
              </defs>
              <path
                v-if="trendSvgPath"
                :d="`M${trendSvgPath.split(' ')[0]} L${trendSvgPath} L300,96 L0,96 Z`"
                fill="url(#trendGrad)"
              />
              <polyline
                v-if="trendSvgPath"
                :points="trendSvgPath"
                fill="none"
                stroke="var(--w-brand)"
                stroke-width="2"
                stroke-linejoin="round"
              />
              <template v-for="(d, i) in (overview?.trend ?? [])" :key="d.date">
                <circle
                  v-if="overview?.trend"
                  :cx="(i / (overview.trend.length - 1)) * 300"
                  :cy="100 - (getTrendValue(d) / Math.max(...overview.trend.map(x => getTrendValue(x)), 1)) * 88 - 4"
                  r="3"
                  fill="var(--w-brand)"
                />
              </template>
            </svg>
            <div class="trend-dates">
              <NText v-for="d in overview?.trend" :key="d.date" depth="3" style="font-size: 10px">
                {{ d.date.slice(5) }}
              </NText>
            </div>
            <div class="trend-download">
              <NButton size="tiny" text @click="downloadTrendChart">
                <template #icon>
                  <NIcon :component="Download" :size="14" />
                </template>
                下载图片
              </NButton>
            </div>
          </div>
        </NSpin>
      </NCard>

      <!-- 热力图 -->
      <NCard class="stats-card" :bordered="false">
        <div class="card-header">
          <NIcon :component="Calendar" :size="18" class="card-header-icon" />
          <span class="card-header-title">365 天写作热力图</span>
        </div>
        <NSpin :show="heatmapLoading">
          <div v-if="heatmapWeeks.length" class="heatmap-wrap">
            <div class="heatmap-grid">
              <div
                v-for="week in heatmapWeeks"
                :key="week[0]?.date"
                class="heatmap-week"
              >
                <div
                  v-for="day in week"
                  :key="day.date"
                  :title="`${day.date}：${day.chars} 字`"
                  class="heatmap-cell"
                  :style="{ background: levelColors[day.level] }"
                />
              </div>
            </div>
            <div class="heatmap-legend">
              <span class="legend-label">少</span>
              <div v-for="(color, i) in levelColors" :key="i" class="heatmap-cell" :style="{ background: color }" />
              <span class="legend-label">多</span>
            </div>
          </div>
          <NEmpty v-else description="暂无热力图数据" />
        </NSpin>
      </NCard>

      <!-- 写作时段热力图 -->
      <NCard class="stats-card" :bordered="false">
        <div class="card-header">
          <NIcon :component="Clock" :size="18" class="card-header-icon" />
          <span class="card-header-title">写作时段热力图</span>
        </div>
        <NSpin :show="timeHeatmapLoading">
          <div v-if="timeHeatmapData.length" class="time-heatmap-wrap">
            <div class="time-heatmap-header">
              <span v-for="h in [0, 6, 12, 18, 23]" :key="h" class="time-heatmap-hour">{{ h }}</span>
            </div>
            <div v-for="wd in 7" :key="wd - 1" class="time-heatmap-row">
              <span class="time-heatmap-label">{{ weekdayLabels[wd - 1] }}</span>
              <div class="time-heatmap-cells">
                <div
                  v-for="h in 24"
                  :key="h - 1"
                  :title="`${weekdayLabels[wd - 1]} ${h - 1}:00 — ${timeHeatmapMap.get(`${wd - 1}-${h - 1}`) ?? 0} 字`"
                  class="time-heatmap-cell"
                  :style="{ background: levelColors[timeCharsToLevel(timeHeatmapMap.get(`${wd - 1}-${h - 1}`) ?? 0)] }"
                />
              </div>
            </div>
            <div class="heatmap-legend">
              <span class="legend-label">少</span>
              <div v-for="(color, i) in levelColors" :key="i" class="heatmap-cell" :style="{ background: color }" />
              <span class="legend-label">多</span>
            </div>
          </div>
          <NEmpty v-else description="暂无时段数据" />
        </NSpin>
      </NCard>

      <!-- 作品进度 -->
      <NCard class="stats-card" :bordered="false">
        <div class="card-header">
          <NIcon :component="BookOpen" :size="18" class="card-header-icon" />
          <span class="card-header-title">作品进度</span>
        </div>
        <NSpin :show="projectLoading">
          <NDataTable
            v-if="projects.length"
            :columns="projectColumns"
            :data="projects"
            :bordered="false"
            size="small"
            class="project-table"
          />
          <NEmpty v-else description="暂无作品数据" />
        </NSpin>
      </NCard>

      <!-- 月度摘要 -->
      <NCard class="stats-card" :bordered="false">
        <div class="card-header">
          <NIcon :component="Calendar" :size="18" class="card-header-icon" />
          <span class="card-header-title">月度摘要</span>
          <NSelect
            :value="selectedMonth"
            :options="monthOptions"
            size="small"
            style="width: 140px; margin-left: auto"
            @update:value="onMonthChange"
          />
        </div>
        <NSpin :show="monthlyLoading">
          <NGrid v-if="monthly" :cols="3" :x-gap="24" class="monthly-grid">
            <NGridItem>
              <NStatistic label="月总字数">
                <template #default>
                  <span class="monthly-value">{{ formatNumber(monthly.totalChars ?? 0) }}</span>
                  <span class="monthly-unit">字</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="活跃天数">
                <template #default>
                  <span class="monthly-value">{{ monthly.activeDays ?? 0 }}</span>
                  <span class="monthly-unit">天</span>
                </template>
              </NStatistic>
            </NGridItem>
            <NGridItem>
              <NStatistic label="日均字数">
                <template #default>
                  <span class="monthly-value">{{ formatNumber(Math.round(monthly.avgCharsPerDay ?? 0)) }}</span>
                  <span class="monthly-unit">字/天</span>
                </template>
              </NStatistic>
            </NGridItem>
          </NGrid>
          <NEmpty v-else description="暂无月度数据" />
        </NSpin>
      </NCard>
    </div>
  </div>
</template>

<style scoped>
.stats-page {
  min-height: 100vh;
  padding-top: var(--w-space-6);
  padding-bottom: calc(var(--w-space-8) + 56px);
  overflow-y: auto;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--w-space-6);
  flex-wrap: wrap;
  gap: var(--w-space-3);
}

.stats-label {
  font-size: var(--w-text-sm);
  color: var(--w-brand);
  font-weight: 500;
  margin-bottom: 6px;
  letter-spacing: 0.04em;
}

.stats-header h1 {
  font-family: var(--w-font-serif);
  font-size: var(--w-text-3xl);
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}

.stats-desc {
  color: var(--w-text-secondary);
  font-size: var(--w-text-base);
}

.streak-tag {
  background: var(--w-warning-soft) !important;
  color: var(--w-warning) !important;
}

.stats-card {
  margin-bottom: var(--w-space-5);
  background: var(--w-bg-secondary) !important;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--w-space-4);
}

.card-header-icon {
  color: var(--w-brand);
}

.card-header-title {
  font-size: var(--w-text-base);
  font-weight: 600;
  color: var(--w-text);
}

/* 今日概览 */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--w-space-4);
  margin-bottom: var(--w-space-5);
}

.overview-item {
  padding: var(--w-space-4);
  background: var(--w-bg-tertiary);
  border: 1px solid var(--w-border-subtle);
  border-radius: var(--w-radius-md);
}

.overview-item--highlight {
  background: var(--w-brand-soft);
  border-color: var(--w-brand-soft);
}

.overview-label {
  display: block;
  font-size: var(--w-text-xs);
  color: var(--w-text-secondary);
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}

.overview-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.overview-value {
  font-size: var(--w-text-xl);
  font-weight: 600;
  color: var(--w-text);
  font-variant-numeric: tabular-nums;
}

.overview-value--large {
  font-size: var(--w-text-2xl);
  color: var(--w-brand);
}

.overview-unit {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

/* 趋势图 */
.trend-section {
  margin-top: var(--w-space-5);
  padding-top: var(--w-space-5);
  border-top: 1px solid var(--w-border-subtle);
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--w-space-3);
  flex-wrap: wrap;
  gap: var(--w-space-2);
}

.trend-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
}

.trend-svg {
  display: block;
}

.trend-dates {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
}

.trend-download {
  text-align: right;
  margin-top: 8px;
}

/* 热力图 */
.heatmap-wrap {
  overflow-x: auto;
  padding-bottom: var(--w-space-2);
}

.heatmap-grid {
  display: flex;
  gap: 3px;
}

.heatmap-week {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heatmap-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: var(--w-space-3);
}

.legend-label {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
}

/* 时段热力图 */
.time-heatmap-wrap {
  overflow-x: auto;
}

.time-heatmap-header {
  display: flex;
  margin-bottom: 4px;
  padding-left: 42px;
}

.time-heatmap-hour {
  flex: 1;
  text-align: center;
  font-size: 10px;
  color: var(--w-text-tertiary);
  min-width: 14px;
}

.time-heatmap-row {
  display: flex;
  align-items: center;
  margin-bottom: 3px;
}

.time-heatmap-label {
  width: 38px;
  font-size: 11px;
  color: var(--w-text-secondary);
  flex-shrink: 0;
}

.time-heatmap-cells {
  flex: 1;
  display: flex;
  gap: 2px;
}

.time-heatmap-cell {
  flex: 1;
  height: 14px;
  min-width: 12px;
  border-radius: 2px;
}

/* 月度摘要 */
.monthly-grid {
  margin-top: var(--w-space-3);
}

.monthly-value {
  font-size: var(--w-text-xl);
  font-weight: 600;
  color: var(--w-text);
  font-variant-numeric: tabular-nums;
}

.monthly-unit {
  font-size: var(--w-text-xs);
  color: var(--w-text-tertiary);
  margin-left: 4px;
}

.project-table :deep(.project-table-title) {
  font-weight: 500;
  color: var(--w-text);
}

@media (max-width: 767px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stats-header {
    flex-direction: column;
  }

  .stats-header h1 {
    font-size: 28px;
  }
}
</style>
