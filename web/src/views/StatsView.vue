<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { BarChart3, CalendarDays, Flame, PenLine } from 'lucide-vue-next'
import {
  getMonthlySummary,
  getProjectProgress,
  getWritingHeatmap,
  getWritingOverview,
} from '@/api/stats'
import type { HeatmapData, MonthlySummary, ProjectProgress, WritingOverview } from '@/api/stats'

const overview = ref<WritingOverview | null>(null)
const heatmap = ref<HeatmapData | null>(null)
const projects = ref<ProjectProgress[]>([])
const monthly = ref<MonthlySummary | null>(null)
const month = ref(new Date().toISOString().slice(0, 7))
const loadError = ref('')

const heatDays = computed(() => heatmap.value?.days.slice(-120) || [])

onMounted(async () => {
  loadError.value = ''
  const [overviewRes, heatRes, projectRes, monthlyRes] = await Promise.allSettled([
    getWritingOverview(),
    getWritingHeatmap(),
    getProjectProgress(),
    getMonthlySummary(month.value),
  ])
  if (overviewRes.status === 'fulfilled') overview.value = overviewRes.value.data.data
  if (heatRes.status === 'fulfilled') heatmap.value = heatRes.value.data.data
  if (projectRes.status === 'fulfilled') projects.value = projectRes.value.data.data
  if (monthlyRes.status === 'fulfilled') monthly.value = monthlyRes.value.data.data
  if ([overviewRes, heatRes, projectRes, monthlyRes].some((item) => item.status === 'rejected')) {
    loadError.value = '部分统计接口加载失败，请稍后刷新。'
  }
})

function pct(value = 0) {
  return `${Math.min(Math.round(value * 100), 100)}%`
}

function level(chars: number) {
  if (chars <= 0) return 0
  if (chars < 500) return 1
  if (chars < 1500) return 2
  if (chars < 3000) return 3
  return 4
}
</script>

<template>
  <div class="ws-page">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Writing Stats</p>
        <h1>写作统计</h1>
        <p>记录今日进度、作品推进和近期热力。</p>
        <p v-if="loadError" class="ws-hint">{{ loadError }}</p>
      </div>
    </section>

    <section class="ws-stat-grid">
      <div class="ws-card ws-stat"><PenLine :size="19" /><strong>{{ overview?.todayChars || 0 }}</strong><span>今日字数</span></div>
      <div class="ws-card ws-stat"><CalendarDays :size="19" /><strong>{{ overview?.dailyGoal || 0 }}</strong><span>每日目标</span></div>
      <div class="ws-card ws-stat"><Flame :size="19" /><strong>{{ overview?.streak || 0 }}</strong><span>连续天数</span></div>
      <div class="ws-card ws-stat"><BarChart3 :size="19" /><strong>{{ overview?.totalChars || 0 }}</strong><span>累计字数</span></div>
    </section>

    <section class="ws-panel">
      <h2>今日目标</h2>
      <div class="progress-track"><span :style="{ width: pct(overview?.todayProgress || 0) }" /></div>
    </section>

    <section class="ws-panel">
      <h2>近 120 天热力</h2>
      <div class="heat-grid">
        <span v-for="day in heatDays" :key="day.date" :class="`heat-${level(day.chars)}`" :title="`${day.date}: ${day.chars} 字`" />
      </div>
    </section>

    <section class="ws-panel">
      <h2>作品进度</h2>
      <div class="table-list">
        <div v-for="item in projects" :key="item.projectId" class="table-row">
          <strong>{{ item.title }}</strong>
          <span>{{ item.todayChars }} / {{ item.dailyCharGoal }} 字</span>
          <div class="progress-track small"><span :style="{ width: pct(item.progress) }" /></div>
        </div>
      </div>
    </section>

    <section class="ws-panel">
      <h2>月度摘要</h2>
      <div class="ws-stat-grid">
        <div class="ws-card ws-stat"><strong>{{ monthly?.totalChars || 0 }}</strong><span>月总字数</span></div>
        <div class="ws-card ws-stat"><strong>{{ monthly?.activeDays || 0 }}</strong><span>活跃天数</span></div>
        <div class="ws-card ws-stat"><strong>{{ Math.round(monthly?.avgCharsPerDay || 0) }}</strong><span>日均字数</span></div>
      </div>
    </section>
  </div>
</template>
