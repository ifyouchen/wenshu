<script setup lang="ts">
/**
 * 配额 Tooltip 组件。
 * 显示当月 AI 字符和改编/审查剩余次数。
 */
import { computed, onMounted } from 'vue'
import { NTooltip, NProgress, NSpace, NText, NIcon } from 'naive-ui'
import { BarChart3, Info } from 'lucide-vue-next'
import { useQuotaStore } from '@/stores/quota'

const quota = useQuotaStore()

onMounted(() => quota.refresh())

const progressStatus = computed(() => {
  if (!quota.quota) return 'default'
  if (quota.charUsagePercent >= 90) return 'error'
  if (quota.charUsagePercent >= 70) return 'warning'
  return 'default'
})

const progressColor = computed(() => {
  if (progressStatus.value === 'error') return 'var(--w-danger)'
  if (progressStatus.value === 'warning') return 'var(--w-warning)'
  return 'var(--w-brand)'
})
</script>

<template>
  <NTooltip placement="bottom-end" :style="{ maxWidth: '280px' }">
    <template #trigger>
      <div class="quota-trigger">
        <NIcon :component="BarChart3" :size="16" class="quota-icon" />
        <div class="quota-bar-wrap">
          <div
            class="quota-bar"
            :style="{
              width: `${quota.charUsagePercent}%`,
              background: progressColor,
            }"
          />
        </div>
      </div>
    </template>
    <div v-if="quota.quota" class="quota-detail">
      <div class="quota-detail-title">本月 AI 字符</div>
      <div class="quota-detail-value">
        {{ quota.quota.usedChars.toLocaleString() }} / {{ quota.quota.limitChars.toLocaleString() }} 字
      </div>
      <NProgress
        type="line"
        :percentage="quota.charUsagePercent"
        :height="6"
        :show-indicator="false"
        :color="progressColor"
        style="margin: 8px 0"
      />
      <div class="quota-detail-row">
        <span class="quota-detail-label">改编/审查次数</span>
        <span class="quota-detail-value">
          {{ quota.quota.usedAdaptations }} / {{ quota.quota.limitAdaptations }} 次
        </span>
      </div>
      <div class="quota-detail-tip">
        <NIcon :component="Info" :size="12" />
        配额用量每次 AI 操作完成后更新
      </div>
    </div>
    <span v-else class="quota-loading">加载中…</span>
  </NTooltip>
</template>

<style scoped>
.quota-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--w-radius-sm);
  transition: background var(--w-transition-fast);
}

.quota-trigger:hover {
  background: var(--w-bg-hover);
}

.quota-icon {
  color: var(--w-text-tertiary);
}

.quota-bar-wrap {
  width: 72px;
  height: 4px;
  background: var(--w-bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
}

.quota-bar {
  height: 100%;
  border-radius: 2px;
  transition: width var(--w-transition-base);
}

.quota-detail {
  padding: 4px;
}

.quota-detail-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
  margin-bottom: 4px;
}

.quota-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--w-border-subtle);
}

.quota-detail-label {
  font-size: var(--w-text-xs);
  color: var(--w-text-secondary);
}

.quota-detail-value {
  font-size: var(--w-text-xs);
  color: var(--w-text);
  font-weight: 500;
}

.quota-detail-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--w-text-tertiary);
}

.quota-loading {
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
}
</style>
