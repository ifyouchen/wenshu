<script setup lang="ts">
/**
 * 异常状态 UI 组件（P8-17）。
 *
 * 覆盖四个异常级别：
 * - A（service-unavailable）：AI 服务不可用，通用故障提示
 * - B（result-unavailable）：结果不可用，生成失败但无配额问题
 * - C（quota-exceeded）：配额不足，含升级套餐入口
 * - D（content-blocked）：内容被安全过滤，含替换说明 Tooltip + 申诉入口
 *
 * 用法：
 * ```html
 * <ErrorStateAlert level="C" message="本月 AI 字符配额已用尽" />
 * <ErrorStateAlert level="D" message="部分内容已被安全替换" :blocked-reason="reason" />
 * ```
 */
import { ref } from 'vue'
import { NAlert, NButton, NTooltip, NSpace, NText, useMessage } from 'naive-ui'
import { submitContentAppeal } from '@/api/safety'

const props = withDefaults(defineProps<{
  /** 异常级别：A/B/C/D。 */
  level: 'A' | 'B' | 'C' | 'D'
  /** 错误描述文字（可选，不传则使用默认描述）。 */
  message?: string
  /** D 级专用：内容被替换的原因说明（如 "含敏感词汇"）。 */
  blockedReason?: string
  /** 被替换的内容原文片段（用于申诉提交，可选）。 */
  blockedContent?: string
}>(), {
  message: '',
  blockedReason: '',
  blockedContent: '',
})

const emit = defineEmits<{
  /** 用户点击关闭后触发。 */
  close: []
  /** C 级：用户点击"升级套餐"触发。 */
  upgrade: []
}>()

const notify = useMessage()
const appealSubmitting = ref(false)
const appealDone = ref(false)
const appealReason = ref('')

/** 默认错误描述（按级别）。 */
const defaultMessages: Record<string, string> = {
  A: 'AI 服务暂时不可用，请稍后重试',
  B: 'AI 生成结果不可用，请重新尝试',
  C: '本月 AI 使用配额已用尽，续写/润色功能暂停',
  D: '生成内容包含敏感内容，已被自动替换',
}

/** Naive UI Alert 类型映射（按级别）。 */
const alertTypes: Record<string, 'default' | 'info' | 'warning' | 'error'> = {
  A: 'default',
  B: 'warning',
  C: 'error',
  D: 'warning',
}

/** 级别对应的图标（emoji）。 */
const levelIcons: Record<string, string> = {
  A: '🔌',
  B: '⚠️',
  C: '📊',
  D: '🛡️',
}

/** 计算展示消息。 */
function displayMessage(): string {
  return props.message || defaultMessages[props.level] || '发生未知错误'
}

/** 提交内容安全申诉（D 级专用）。 */
async function handleSubmitAppeal() {
  if (!appealReason.value.trim()) {
    notify.warning('请输入申诉理由')
    return
  }
  appealSubmitting.value = true
  try {
    const content = props.blockedContent || '（被替换的 AI 输出内容）'
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
  <NAlert
    :type="alertTypes[level]"
    :title="`${levelIcons[level]} ${level} 级异常：${displayMessage()}`"
    closable
    style="margin: 8px 16px"
    @close="$emit('close')"
  >
    <!-- A 级：服务不可用 -->
    <template v-if="level === 'A'">
      <NText depth="3" style="font-size: 13px">
        AI 服务正在维护或遇到临时故障，请稍等片刻后重试。
        如持续出现，请检查网络连接或联系支持。
      </NText>
    </template>

    <!-- B 级：结果不可用 -->
    <template v-else-if="level === 'B'">
      <NText depth="3" style="font-size: 13px">
        本次 AI 生成未能返回有效内容，可能是提示词或上下文不足导致。
        请调整写作指示后重新尝试。
      </NText>
    </template>

    <!-- C 级：配额不足 -->
    <template v-else-if="level === 'C'">
      <NSpace vertical :size="8">
        <NText depth="3" style="font-size: 13px">
          当前套餐的月度 AI 字符配额已耗尽。
          下月初自动重置，或升级套餐获取更多配额。
        </NText>
        <NSpace :size="8">
          <NButton size="small" type="primary" @click="$emit('upgrade')">
            升级套餐
          </NButton>
          <NButton size="small" @click="$router.push('/settings?tab=sub')">
            查看用量
          </NButton>
        </NSpace>
      </NSpace>
    </template>

    <!-- D 级：内容被替换 + 申诉入口 -->
    <template v-else-if="level === 'D'">
      <NSpace vertical :size="10">
        <!-- 替换说明 Tooltip -->
        <NTooltip placement="top">
          <template #trigger>
            <NText
              style="font-size: 13px; cursor: help; text-decoration: underline dotted"
              type="warning"
            >
              🛡️ 为什么内容被替换？{{ blockedReason ? `（${blockedReason}）` : '' }}
            </NText>
          </template>
          <div style="max-width: 280px">
            <p style="margin: 0 0 6px; font-weight: 600">内容安全提示</p>
            <p style="margin: 0; font-size: 12px; opacity: 0.9">
              {{ blockedReason || 'AI 输出内容经系统检测，含有可能违规的表达，已进行自动替换。' }}
            </p>
            <p style="margin: 6px 0 0; font-size: 12px; opacity: 0.7">
              如认为属于误判，请通过下方申诉入口反馈。
            </p>
          </div>
        </NTooltip>

        <!-- 申诉入口 -->
        <div v-if="!appealDone">
          <NText depth="3" style="font-size: 12px; display: block; margin-bottom: 6px">
            认为误判？提交申诉说明理由：
          </NText>
          <NSpace align="center" :size="6">
            <input
              v-model="appealReason"
              placeholder="简述理由（如：这是文学创作内容）"
              style="flex: 1; padding: 4px 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 12px; min-width: 200px"
            />
            <NButton
              size="small"
              type="info"
              :loading="appealSubmitting"
              @click="handleSubmitAppeal"
            >
              提交申诉
            </NButton>
          </NSpace>
        </div>
        <NText v-else type="success" style="font-size: 12px">
          ✅ 申诉已提交，我们将尽快审核并回复。
        </NText>
      </NSpace>
    </template>
  </NAlert>
</template>
