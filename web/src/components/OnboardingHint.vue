<script setup lang="ts">
/**
 * 渐进式引导提示组件（P8-18）。
 *
 * 轻量非阻塞的引导提示：
 * - 有图标、标题（可选）和描述文字
 * - 右上角关闭按钮，点击关闭并触发 close 事件
 * - 支持可选的操作按钮（如"我知道了"）
 * - 不遮挡底层内容（非 Modal）
 *
 * @example
 * <OnboardingHint
 *   icon="✨"
 *   title="选中文字唤起 AI"
 *   description="选中任意文字后，会出现 AI 浮窗。"
 *   action-label="我知道了"
 *   @close="ob.markDone('first-ai-select')"
 * />
 */

const props = withDefaults(
  defineProps<{
    /** 图标（emoji 或文字）。 */
    icon?: string
    /** 标题（可选）。 */
    title?: string
    /** 说明文字（必填）。 */
    description: string
    /** 操作按钮文字（可选，不传则不显示操作按钮）。 */
    actionLabel?: string
    /** 外观变体：info / success / welcome。 */
    variant?: 'info' | 'success' | 'welcome'
  }>(),
  {
    icon: '💡',
    variant: 'info',
    actionLabel: '我知道了',
  },
)

const emit = defineEmits<{
  /** 用户点击关闭按钮或操作按钮后触发。 */
  close: []
  /** 用户点击操作按钮时触发（可用于执行具体动作）。 */
  action: []
}>()

function handleAction() {
  emit('action')
  emit('close')
}
</script>

<template>
  <div class="ob-hint" :class="`ob-hint--${variant}`">
    <!-- 关闭按钮 -->
    <button class="ob-hint-close" @click="$emit('close')" title="关闭提示">×</button>

    <!-- 内容区 -->
    <div class="ob-hint-content">
      <!-- 图标 -->
      <span class="ob-hint-icon">{{ props.icon }}</span>

      <!-- 文字区 -->
      <div class="ob-hint-text">
        <div v-if="props.title" class="ob-hint-title">{{ props.title }}</div>
        <div class="ob-hint-desc">{{ props.description }}</div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div v-if="props.actionLabel" class="ob-hint-actions">
      <button class="ob-hint-action-btn" @click="handleAction">
        {{ props.actionLabel }}
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ─── 提示卡片容器 ─── */
.ob-hint {
  position: relative;
  border-radius: 10px;
  padding: 14px 36px 14px 14px;
  margin: 12px 0;
  border-left: 3px solid;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  animation: ob-fade-in 0.3s ease;
  background: var(--ob-bg, #f0f7ff);
  border-color: var(--ob-border, #3b82f6);
}

/* ─── 变体颜色 ─── */
.ob-hint--info {
  --ob-bg: #f0f7ff;
  --ob-border: #3b82f6;
  --ob-icon-bg: rgba(59, 130, 246, 0.12);
  --ob-btn-color: #3b82f6;
}
.ob-hint--success {
  --ob-bg: #f0fdf4;
  --ob-border: #22c55e;
  --ob-icon-bg: rgba(34, 197, 94, 0.12);
  --ob-btn-color: #16a34a;
}
.ob-hint--welcome {
  --ob-bg: linear-gradient(135deg, #f5f3ff 0%, #fdf4ff 100%);
  --ob-border: #8b5cf6;
  --ob-icon-bg: rgba(139, 92, 246, 0.12);
  --ob-btn-color: #7c3aed;
}

/* ─── 关闭按钮 ─── */
.ob-hint-close {
  position: absolute;
  top: 8px;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  opacity: 0.4;
  padding: 2px 4px;
  border-radius: 4px;
  transition: opacity 0.15s;
  color: inherit;
}
.ob-hint-close:hover { opacity: 0.8; }

/* ─── 内容区 ─── */
.ob-hint-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

/* ─── 图标 ─── */
.ob-hint-icon {
  font-size: 22px;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ob-icon-bg, rgba(0, 0, 0, 0.06));
  border-radius: 8px;
}

/* ─── 文字区 ─── */
.ob-hint-text { flex: 1; }
.ob-hint-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 3px;
}
.ob-hint-desc {
  font-size: 13px;
  line-height: 1.55;
  opacity: 0.8;
}

/* ─── 操作按钮 ─── */
.ob-hint-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
.ob-hint-action-btn {
  background: none;
  border: 1px solid var(--ob-btn-color, #3b82f6);
  color: var(--ob-btn-color, #3b82f6);
  padding: 4px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.ob-hint-action-btn:hover {
  background: var(--ob-btn-color, #3b82f6);
  color: #fff;
}

/* ─── 入场动画 ─── */
@keyframes ob-fade-in {
  from { opacity: 0; transform: translateY(-6px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
