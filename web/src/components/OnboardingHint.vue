<script setup lang="ts">
/**
 * 渐进式引导提示组件。
 *
 * 轻量非阻塞的引导提示：
 * - 有图标、标题（可选）和描述文字
 * - 右上角关闭按钮，点击关闭并触发 close 事件
 * - 支持可选的操作按钮
 * - 不遮挡底层内容（非 Modal）
 */
import { computed } from 'vue'
import { NIcon } from 'naive-ui'
import { Lightbulb, CheckCircle2, Info, type LucideIcon } from 'lucide-vue-next'

const props = withDefaults(
  defineProps<{
    icon?: 'lightbulb' | 'check' | 'info'
    title?: string
    description: string
    actionLabel?: string
    variant?: 'info' | 'success' | 'welcome'
  }>(),
  {
    icon: 'info',
    variant: 'info',
    actionLabel: '我知道了',
  },
)

const emit = defineEmits<{
  close: []
  action: []
}>()

const iconMap: Record<string, LucideIcon> = {
  lightbulb: Lightbulb,
  check: CheckCircle2,
  info: Info,
}

const currentIcon = computed(() => iconMap[props.icon] || Info)

function handleAction() {
  emit('action')
  emit('close')
}
</script>

<template>
  <div class="ob-hint" :class="`ob-hint--${variant}`">
    <button class="ob-hint-close" @click="$emit('close')" title="关闭提示">×</button>

    <div class="ob-hint-content">
      <span class="ob-hint-icon">
        <NIcon :component="currentIcon" :size="18" />
      </span>

      <div class="ob-hint-text">
        <div v-if="props.title" class="ob-hint-title">{{ props.title }}</div>
        <div class="ob-hint-desc">{{ props.description }}</div>
      </div>
    </div>

    <div v-if="props.actionLabel" class="ob-hint-actions">
      <button class="ob-hint-action-btn" @click="handleAction">
        {{ props.actionLabel }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.ob-hint {
  position: relative;
  border-radius: var(--w-radius-md);
  padding: 16px 44px 16px 16px;
  margin: 12px 0;
  border-left: 3px solid;
  animation: ob-fade-in 0.3s ease;
  background: var(--ob-bg);
  border-color: var(--ob-border);
}

.ob-hint--info {
  --ob-bg: var(--w-brand-soft);
  --ob-border: var(--w-brand);
  --ob-icon-color: var(--w-brand);
  --ob-btn-color: var(--w-brand);
}

.ob-hint--success {
  --ob-bg: var(--w-success-soft);
  --ob-border: var(--w-success);
  --ob-icon-color: var(--w-success);
  --ob-btn-color: var(--w-success);
}

.ob-hint--welcome {
  --ob-bg: var(--w-bg-tertiary);
  --ob-border: var(--w-brand);
  --ob-icon-color: var(--w-brand);
  --ob-btn-color: var(--w-brand);
}

.ob-hint-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  color: var(--w-text-tertiary);
  border-radius: var(--w-radius-sm);
  transition: all var(--w-transition-fast);
}

.ob-hint-close:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.ob-hint-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.ob-hint-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--w-bg);
  border-radius: var(--w-radius-sm);
  color: var(--ob-icon-color);
}

.ob-hint-text {
  flex: 1;
}

.ob-hint-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
  margin-bottom: 4px;
}

.ob-hint-desc {
  font-size: var(--w-text-sm);
  line-height: 1.6;
  color: var(--w-text-secondary);
}

.ob-hint-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.ob-hint-action-btn {
  background: var(--ob-btn-color);
  border: 1px solid var(--ob-btn-color);
  color: #fff;
  padding: 6px 16px;
  border-radius: var(--w-radius-sm);
  font-size: var(--w-text-xs);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--w-transition-fast);
  font-family: inherit;
}

.ob-hint-action-btn:hover {
  opacity: 0.9;
}

@keyframes ob-fade-in {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
