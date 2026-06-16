<script setup lang="ts">
/**
 * AI 浮窗组件。
 *
 * - 文本选中时显示，提供续写/润色入口
 * - SSE 续写使用 fetch ReadableStream + RAF 批量写入 TipTap
 * - AI 生成内容带品牌色标识
 */
import { ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { NButton, NPopover, NSpace, NInput, NSpin, NIcon, useMessage } from 'naive-ui'
import { Sparkles, Wand2, Paintbrush, Check, X } from 'lucide-vue-next'
import { getAccessToken } from '@/api/client'
import ErrorStateAlert from '@/components/ErrorStateAlert.vue'
import { acceptAiContent } from '@/api/project'

const props = defineProps<{
  editor: Editor | undefined
  chapterId: string | undefined
  visible: boolean
}>()

const emit = defineEmits<{
  streamStart: []
  streamEnd: []
}>()

const message = useMessage()
const isStreaming = ref(false)
const showOptions = ref(false)
const instruction = ref('')

const aiContentInserted = ref(false)
const hasPendingAiContent = ref(false)

const errorLevel = ref<'A' | 'B' | 'C' | 'D' | null>(null)
const errorMessage = ref('')
const blockedContent = ref('')

function inferErrorLevel(err: string, httpStatus?: number): 'A' | 'B' | 'C' | 'D' {
  const lower = err.toLowerCase()
  if (httpStatus === 429 || lower.includes('配额') || lower.includes('额度') ||
      lower.includes('rate') || lower.includes('limit')) return 'C'
  if (lower.includes('安全') || lower.includes('替换') || lower.includes('违规') ||
      lower.includes('blocked') || lower.includes('content')) return 'D'
  if (httpStatus === 503 || lower.includes('503') || lower.includes('不可用') ||
      lower.includes('unavailable') || lower.includes('service')) return 'A'
  return 'B'
}

function clearError() {
  errorLevel.value = null
  errorMessage.value = ''
  blockedContent.value = ''
}

async function startContinue() {
  if (!props.editor || !props.chapterId) {
    message.warning('请先选择章节')
    return
  }
  const token = getAccessToken()
  if (!token) { message.error('请先登录'); return }

  clearError()
  isStreaming.value = true
  showOptions.value = false
  emit('streamStart')

  const { to } = props.editor.state.selection
  props.editor.commands.focus()
  props.editor.commands.setTextSelection(to)
  props.editor.commands.insertContent('\n')

  const tokenBuffer: string[] = []
  let rafId: number | null = null

  function flushTokens() {
    if (!tokenBuffer.length) return
    const chunk = tokenBuffer.splice(0).join('')
    props.editor!.commands.insertContent(`<span data-ai="true">${chunk}</span>`)
    rafId = null
  }

  function scheduleFlush() {
    if (!rafId) {
      rafId = requestAnimationFrame(flushTokens)
    }
  }

  const url = `/api/v1/novel/continue?chapterId=${props.chapterId}` +
    (instruction.value ? `&instruction=${encodeURIComponent(instruction.value)}` : '')

  try {
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` },
    })

    if (!response.ok) {
      const level = inferErrorLevel(`HTTP ${response.status}`, response.status)
      const msg = response.status === 429
        ? '本月 AI 字符配额已用尽'
        : response.status === 503
          ? 'AI 服务暂时不可用，请稍后重试'
          : `AI 服务请求失败（${response.status}）`
      errorLevel.value = level
      errorMessage.value = msg
      return
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let partial = ''
    let lastEventType = 'token'

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      partial += decoder.decode(value, { stream: true })
      const lines = partial.split('\n')
      partial = lines.pop() ?? ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          lastEventType = line.slice(6).trim()
          continue
        }
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (!data) continue

        if (lastEventType === 'token') {
          if (!aiContentInserted.value) aiContentInserted.value = true
          tokenBuffer.push(data)
          scheduleFlush()
        } else if (lastEventType === 'done') {
          break
        } else if (lastEventType === 'error' || lastEventType === 'timeout') {
          const level = inferErrorLevel(data)
          errorLevel.value = level
          errorMessage.value = data
          if (level === 'D') blockedContent.value = data
          break
        }
        lastEventType = 'token'
      }
    }

    if (rafId) cancelAnimationFrame(rafId)
    flushTokens()

  } catch (err: unknown) {
    const errMsg = (err as Error).message || 'AI 续写请求失败'
    errorLevel.value = 'A'
    errorMessage.value = errMsg
  } finally {
    isStreaming.value = false
    emit('streamEnd')
    if (aiContentInserted.value && !errorLevel.value) {
      hasPendingAiContent.value = true
    }
    instruction.value = ''
  }
}

async function handleAcceptAi() {
  if (!props.editor || !props.chapterId) return

  const editorDom = props.editor.view.dom as HTMLElement
  const aiSpans = editorDom.querySelectorAll('span[data-ai="true"]')
  let acceptedCharsCount = 0
  aiSpans.forEach(span => {
    acceptedCharsCount += (span.textContent ?? '').length
  })

  const editorContent = props.editor.getHTML()

  try {
    await acceptAiContent(props.chapterId, acceptedCharsCount, editorContent)
    message.success('已接受 AI 内容，快照已创建')
  } catch {
    message.warning('接受 AI 内容时记录失败，但内容已保留')
  }

  editorDom.querySelectorAll('span[data-ai]').forEach(span => {
    span.removeAttribute('data-ai')
  })

  hasPendingAiContent.value = false
  aiContentInserted.value = false
}

function handleRejectAi() {
  if (!props.editor) return

  const editorDom = props.editor.view.dom as HTMLElement
  const aiSpans = editorDom.querySelectorAll('span[data-ai="true"]')
  aiSpans.forEach(span => {
    span.remove()
  })

  const newContent = editorDom.innerHTML
  props.editor.commands.setContent(newContent)

  message.info('已放弃 AI 内容')
  hasPendingAiContent.value = false
  aiContentInserted.value = false
}

async function startPolish() {
  message.info('润色功能开发中')
}
</script>

<template>
  <div class="ai-float-root">
    <ErrorStateAlert
      v-if="errorLevel"
      :level="errorLevel"
      :message="errorMessage"
      :blocked-content="blockedContent"
      class="ai-error-alert"
      @close="clearError"
      @upgrade="$router.push('/settings?tab=sub')"
    />

    <div v-if="visible || isStreaming" class="ai-float-wrap">
      <NPopover
        v-model:show="showOptions"
        trigger="click"
        placement="bottom-start"
        :disabled="isStreaming"
      >
        <template #trigger>
          <NButton
            type="primary"
            size="small"
            :loading="isStreaming"
            :disabled="isStreaming"
            class="ai-main-btn"
          >
            <template #icon>
              <NIcon :component="Sparkles" :size="14" />
            </template>
            AI
          </NButton>
        </template>

        <div class="ai-options">
          <div class="ai-options-title">AI 写作辅助</div>
          <NInput
            v-model:value="instruction"
            size="small"
            placeholder="写作指示（可选）"
            class="ai-instruction-input"
            @keydown.enter="startContinue"
          />
          <div class="ai-options-hint">预计消耗约 800-1500 字配额</div>
          <NSpace :size="8">
            <NButton size="small" type="primary" @click="startContinue">
              <template #icon>
                <NIcon :component="Wand2" :size="14" />
              </template>
              续写
            </NButton>
            <NButton size="small" @click="startPolish">
              <template #icon>
                <NIcon :component="Paintbrush" :size="14" />
              </template>
              润色
            </NButton>
          </NSpace>
          <div class="ai-options-note">AI 内容需手动接受后计入配额</div>
        </div>
      </NPopover>

      <NSpin v-if="isStreaming" size="small" class="ai-loading" />
    </div>

    <div v-if="hasPendingAiContent && !isStreaming" class="ai-accept-bar">
      <NIcon :component="Sparkles" :size="14" class="ai-accept-icon" />
      <span class="ai-accept-text">AI 内容已生成</span>
      <NButton size="small" type="primary" @click="handleAcceptAi">
        <template #icon>
          <NIcon :component="Check" :size="14" />
        </template>
        接受
      </NButton>
      <NButton size="small" class="ai-reject-btn" @click="handleRejectAi">
        <template #icon>
          <NIcon :component="X" :size="14" />
        </template>
        放弃
      </NButton>
    </div>
  </div>
</template>

<style scoped>
.ai-float-root {
  position: absolute;
  bottom: 80px;
  right: 24px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.ai-error-alert {
  width: 340px;
  margin: 0 0 8px !important;
}

.ai-float-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-main-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: var(--w-shadow-md);
}

.ai-loading {
  color: var(--w-brand);
}

.ai-options {
  width: 240px;
  padding: 4px;
}

.ai-options-title {
  font-size: var(--w-text-sm);
  font-weight: 600;
  color: var(--w-text);
  margin-bottom: 10px;
}

.ai-instruction-input {
  margin-bottom: 8px;
}

.ai-options-hint,
.ai-options-note {
  font-size: 11px;
  color: var(--w-text-tertiary);
  margin: 8px 0;
}

.ai-accept-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--w-bg-elevated);
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  padding: 8px 12px;
  box-shadow: var(--w-shadow-md);
}

.ai-accept-icon {
  color: var(--w-brand);
}

.ai-accept-text {
  font-size: var(--w-text-sm);
  color: var(--w-text-secondary);
  margin-right: 4px;
}

.ai-reject-btn {
  color: var(--w-text-secondary) !important;
}

.ai-reject-btn:hover {
  color: var(--w-danger) !important;
}

@media (max-width: 767px) {
  .ai-float-root {
    bottom: 72px;
    right: 12px;
  }

  .ai-error-alert {
    width: calc(100vw - 24px);
  }

  .ai-accept-bar {
    flex-wrap: wrap;
    max-width: calc(100vw - 24px);
  }
}
</style>
