<script setup lang="ts">
/**
 * 配额 Tooltip 组件（P8-05）。
 * 显示当月 AI 字符和改编/审查剩余次数，
 * 提示"配额用量每次 AI 操作完成后更新"。
 */
import { onMounted } from 'vue'
import { NTooltip, NProgress, NSpace, NText } from 'naive-ui'
import { useQuotaStore } from '@/stores/quota'

const quota = useQuotaStore()

onMounted(() => quota.refresh())
</script>

<template>
  <NTooltip placement="bottom-end" :style="{ maxWidth: '260px' }">
    <template #trigger>
      <div class="quota-trigger">
        <NProgress
          type="line"
          :percentage="quota.charUsagePercent"
          :height="6"
          :border-radius="3"
          :show-indicator="false"
          :status="quota.charUsagePercent >= 90 ? 'error' : quota.charUsagePercent >= 70 ? 'warning' : 'success'"
          style="width: 80px"
        />
        <NText depth="3" style="font-size: 12px; margin-left: 6px">配额</NText>
      </div>
    </template>
    <div v-if="quota.quota">
      <NSpace vertical :size="6">
        <NText strong>本月 AI 字符</NText>
        <NText>已用 {{ quota.quota.usedChars.toLocaleString() }} /
               {{ quota.quota.limitChars.toLocaleString() }} 字</NText>
        <NProgress
          type="line"
          :percentage="quota.charUsagePercent"
          :height="8"
          :show-indicator="false"
          :status="quota.charUsagePercent >= 90 ? 'error' : 'default'"
        />
        <NText strong>改编/审查次数</NText>
        <NText>已用 {{ quota.quota.usedAdaptations }} / {{ quota.quota.limitAdaptations }} 次</NText>
        <NText depth="3" style="font-size: 11px">
          ℹ 配额用量每次 AI 操作完成后更新
        </NText>
      </NSpace>
    </div>
    <span v-else>加载中…</span>
  </NTooltip>
</template>

<style scoped>
.quota-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.quota-trigger:hover {
  background: rgba(0, 0, 0, 0.06);
}
</style>
