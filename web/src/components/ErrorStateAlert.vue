<script setup lang="ts">
/**
 * 异常状态 UI 组件。
 *
 * 覆盖四个异常级别：
 * - A（service-unavailable）：创作辅助服务不可用
 * - B（result-unavailable）：结果不可用
 * - C（quota-exceeded）：配额不足
 * - D（content-blocked）：内容被安全过滤
 */
import { ref, computed } from 'vue'
import { NButton, NTooltip, NSpace, NInput, NIcon, useMessage } from 'naive-ui'
import { AlertTriangle, WifiOff, XCircle, ShieldAlert, BarChart3 } from 'lucide-vue-next'
import { submitContentAppeal } from '@/api/safety'
import { useUpgradeModal } from '@/composables/useUpgradeModal'

const props = withDefaults(defineProps<{
  level: 'A' | 'B' | 'C' | 'D'
  message?: string
  blockedReason?: string
  blockedContent?: string
}>(), {
  message: '',
  blockedReason: '',
  blockedContent: '',
})

const emit = defineEmits<{
  close: []
  upgrade: []
}>()

const notify = useMessage()
const { openUpgrade } = useUpgradeModal()
const appealSubmitting = ref(false)
const appealDone = ref(false)
const appealReason = ref('')

const defaultMessages: Record<string, string> = {
  A: '创作辅助服务暂时不可用，请稍后重试',
  B: '生成结果不可用，请重新尝试',
  C: '本月创作辅助配额已用尽',
  D: '生成内容包含敏感内容，已被自动替换',
}

const levelMeta = computed(() => {
  switch (props.level) {
    case 'A':
      return {
        icon: WifiOff,
        title: '服务不可用',
        desc: '创作辅助服务正在维护或遇到临时故障，请稍等片刻后重试。如持续出现，请检查网络连接。',
        borderColor: 'var(--w-border-default)',
        bgColor: 'var(--w-bg-tertiary)',
        iconColor: 'var(--w-text-tertiary)',
      }
    case 'B':
      return {
        icon: XCircle,
        title: '生成失败',
        desc: '本次生成未能返回有效内容，可能是指示或上下文不足导致。请调整写作指示后重新尝试。',
        borderColor: 'var(--w-warning)',
        bgColor: 'var(--w-warning-soft)',
        iconColor: 'var(--w-warning)',
      }
    case 'C':
      return {
        icon: BarChart3,
        title: '配额不足',
        desc: '当前套餐的月度创作辅助字数已耗尽。下月初自动重置，或升级套餐获取更多配额。',
        borderColor: 'var(--w-danger)',
        bgColor: 'var(--w-danger-soft)',
        iconColor: 'var(--w-danger)',
      }
    case 'D':
    default:
      return {
        icon: ShieldAlert,
        title: '内容安全过滤',
        desc: '输出内容经系统检测，含有可能违规的表达，已进行自动替换。',
        borderColor: 'var(--w-warning)',
        bgColor: 'var(--w-warning-soft)',
        iconColor: 'var(--w-warning)',
      }
  }
})

const displayMessage = computed(() => props.message || defaultMessages[props.level] || '发生未知错误')

async function handleSubmitAppeal() {
  if (!appealReason.value.trim()) {
    notify.warning('请输入申诉理由')
    return
  }
  appealSubmitting.value = true
  try {
    const content = props.blockedContent || '（被替换的输出内容）'
    await submitContentAppeal(content, appealReason.value.trim())
    appealDone.value = true
    notify.success('申诉已提交，我们将在 3 个工作日内审核')
  } catch {
    notify.error('申诉提交失败，请稍后重试')
  } finally {
    appealSubmitting.value = false
  }
}
</script>

<template>
  <div
    class="error-alert"
    :style="{
      background: levelMeta.bgColor,
      borderColor: levelMeta.borderColor,
    }"
  >
    <div class="error-alert-header">
      <div class="error-alert-icon" :style="{ color: levelMeta.iconColor }">
        <NIcon :component="levelMeta.icon" :size="20" />
      </div>
      <div class="error-alert-main">
        <div class="error-alert-title">{{ levelMeta.title }}</div>
        <div class="error-alert-message">{{ displayMessage }}</div>
      </div>
      <button class="error-alert-close" @click="emit('close')">×</button>
    </div>

    <div class="error-alert-body">
      <p class="error-alert-desc">{{ levelMeta.desc }}</p>

      <!-- C 级：配额不足 -->
      <NSpace v-if="level === 'C'" :size="10" align="center" style="margin-top: 12px">
        <NButton size="small" type="primary" @click="openUpgrade('quota-chars')">
          升级套餐
        </NButton>
        <NButton size="small" ghost @click="$router.push('/settings?tab=sub')">
          查看用量
        </NButton>
      </NSpace>

      <!-- D 级：申诉入口 -->
      <template v-else-if="level === 'D'">
        <div class="appeal-section">
          <NTooltip placement="top" trigger="hover">
            <template #trigger>
              <span class="appeal-trigger">
                <AlertTriangle :size="14" />
                为什么内容被替换？
              </span>
            </template>
            <div style="max-width: 280px; font-size: 12px; line-height: 1.6">
              <p style="margin: 0 0 6px; font-weight: 600">内容安全提示</p>
              <p style="margin: 0; opacity: 0.9">
                {{ blockedReason || '输出内容经系统检测，含有可能违规的表达，已进行自动替换。' }}
              </p>
              <p style="margin: 6px 0 0; opacity: 0.7">如认为属于误判，请提交申诉反馈。</p>
            </div>
          </NTooltip>

          <div v-if="!appealDone" class="appeal-form">
            <NInput
              v-model:value="appealReason"
              size="small"
              placeholder="简述理由（如：这是文学创作内容）"
              :style="{ flex: 1 }"
            />
            <NButton
              size="small"
              type="primary"
              :loading="appealSubmitting"
              @click="handleSubmitAppeal"
            >
              提交申诉
            </NButton>
          </div>
          <div v-else class="appeal-done">
            申诉已提交，我们将尽快审核并回复。
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.error-alert {
  border-left: 3px solid;
  border-radius: var(--w-radius-md);
  padding: 16px;
  margin: 12px 16px;
}

.error-alert-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.error-alert-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.error-alert-main {
  flex: 1;
  min-width: 0;
}

.error-alert-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
  margin-bottom: 2px;
}

.error-alert-message {
  font-size: var(--w-text-base);
  color: var(--w-text-secondary);
  line-height: 1.5;
}

.error-alert-close {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--w-radius-sm);
  color: var(--w-text-tertiary);
  font-size: 18px;
  line-height: 1;
  transition: all var(--w-transition-fast);
}

.error-alert-close:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.error-alert-body {
  margin-top: 10px;
  padding-left: 32px;
}

.error-alert-desc {
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
  line-height: 1.6;
  margin: 0;
}

.appeal-section {
  margin-top: 12px;
}

.appeal-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--w-text-sm);
  color: var(--w-warning);
  cursor: help;
  text-decoration: underline;
  text-decoration-style: dotted;
  text-underline-offset: 3px;
  margin-bottom: 10px;
}

.appeal-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.appeal-done {
  font-size: var(--w-text-sm);
  color: var(--w-success);
}

@media (max-width: 767px) {
  .error-alert {
    margin: 8px;
    padding: 12px;
  }

  .error-alert-body {
    padding-left: 0;
  }

  .appeal-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
