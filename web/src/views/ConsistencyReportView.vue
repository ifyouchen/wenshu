<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlertTriangle, CheckCircle2, RotateCcw } from 'lucide-vue-next'
import { getConsistencyReport, updateItemStatus } from '@/api/consistency'
import type { ConsistencyReport, ConsistencyReportItem } from '@/api/consistency'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const report = ref<ConsistencyReport | null>(null)
const loading = ref(false)
const active = ref('all')

const groups = computed(() => {
  const items = report.value?.items || []
  return {
    all: items,
    open: items.filter((item) => item.status === 'open'),
    handled: items.filter((item) => item.status === 'handled'),
    ignored: items.filter((item) => item.status === 'ignored'),
  }
})
const visibleItems = computed(() => groups.value[active.value as keyof typeof groups.value] || [])

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await getConsistencyReport(String(route.params.reportId))
    report.value = res.data.data
  } catch {
    toast.error('报告加载失败')
  } finally {
    loading.value = false
  }
}

async function setStatus(item: ConsistencyReportItem, status: 'open' | 'handled' | 'ignored') {
  await updateItemStatus(item.id, status)
  item.status = status
}
</script>

<template>
  <div class="ws-page">
    <section class="ws-page__head">
      <div>
        <p class="ws-eyebrow">Consistency</p>
        <h1>一致性报告</h1>
        <p v-if="report">共 {{ report.totalItems }} 条，待处理 {{ report.openItems }} 条。</p>
      </div>
      <div class="ws-actions">
        <button v-if="report?.projectId" class="ws-button" type="button" @click="router.push(`/projects/${report.projectId}/editor`)">打开编辑器</button>
        <button class="ws-button" type="button" @click="load"><RotateCcw :size="16" />刷新</button>
      </div>
    </section>

    <div class="ws-tabs">
      <button :class="{ active: active === 'all' }" @click="active = 'all'">全部 {{ groups.all.length }}</button>
      <button :class="{ active: active === 'open' }" @click="active = 'open'">待处理 {{ groups.open.length }}</button>
      <button :class="{ active: active === 'handled' }" @click="active = 'handled'">已处理 {{ groups.handled.length }}</button>
      <button :class="{ active: active === 'ignored' }" @click="active = 'ignored'">忽略 {{ groups.ignored.length }}</button>
    </div>

    <section v-if="loading" class="ws-empty">加载中...</section>
    <section v-else-if="!visibleItems.length" class="ws-empty">
      <CheckCircle2 :size="32" />
      <span>没有对应状态的问题。</span>
    </section>
    <section v-else class="issue-list">
      <article v-for="item in visibleItems" :key="item.id" class="ws-card issue-card">
        <div class="compact-card__title">
          <span class="ws-badge">{{ item.type || 'other' }}</span>
          <span class="ws-badge" :class="{ success: item.status === 'handled', danger: item.status === 'open' }">
            {{ item.status === 'open' ? '待处理' : item.status === 'handled' ? '已处理' : '已忽略' }}
          </span>
        </div>
        <p><AlertTriangle :size="15" />{{ item.description }}</p>
        <small v-if="item.suggestion">建议：{{ item.suggestion }}</small>
        <div class="ws-actions">
          <button class="ws-button" type="button" @click="setStatus(item, 'handled')">已处理</button>
          <button class="ws-button" type="button" @click="setStatus(item, 'ignored')">忽略</button>
          <button class="ws-button" type="button" @click="setStatus(item, 'open')">重新打开</button>
        </div>
      </article>
    </section>
  </div>
</template>
