<script setup lang="ts">
/**
 * 创作辅助浮窗组件。
 *
 * - 文本选中时显示，提供续写/润色入口
 * - SSE 续写使用 fetch ReadableStream + RAF 批量写入 TipTap
 * - 生成内容带低干扰标识
 */
import { ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { NButton, NPopover, NSpace, NInput, NSpin, NIcon, useMessage } from 'naive-ui'
import { Wand2, Paintbrush, Check, X } from 'lucide-vue-next'
import { getAccessToken } from '@/api/client'
import ErrorStateAlert from '@/components/ErrorStateAlert.vue'
import { acceptAiContent } from '@/api/project'
import { polishAdvanced, polishBasic, polishStyle, type PolishResult } from '@/api/polish'

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
const polishLoading = ref(false)
const showOptions = ref(false)
const instruction = ref('')
const polishResult = ref<PolishResult | null>(null)
const selectedRange = ref<{ from: number; to: number } | null>(null)
const selectedText = ref('')

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
        ? '本月创作辅助字数已用尽'
        : response.status === 503
          ? '创作辅助服务暂时不可用，请稍后重试'
          : `创作辅助请求失败（${response.status}）`
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
    const errMsg = (err as Error).message || '续写请求失败'
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
    message.success('已接受辅助内容，快照已创建')
  } catch {
    message.warning('辅助内容记录失败，但内容已保留')
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

  message.info('已放弃辅助内容')
  hasPendingAiContent.value = false
  aiContentInserted.value = false
}

function getSelectedText(): string {
  if (!props.editor) return ''
  const { from, to } = props.editor.state.selection
  selectedRange.value = { from, to }
  selectedText.value = props.editor.state.doc.textBetween(from, to, '\n')
  return selectedText.value
}

async function startPolish(mode: 'basic' | 'advanced' | 'style') {
  if (!props.editor) return
  const text = getSelectedText()
  if (!text.trim()) {
    message.warning('请先选中需要处理的文字')
    return
  }
  polishLoading.value = true
  showOptions.value = false
  try {
    if (mode === 'basic') {
      const res = await polishBasic(text)
      polishResult.value = res.data.data
    } else if (mode === 'advanced') {
      const res = await polishAdvanced(text, instruction.value || undefined)
      polishResult.value = res.data.data
    } else {
      const res = await polishStyle(text, instruction.value || '更克制、自然、保留原有叙事信息')
      polishResult.value = res.data.data
    }
  } catch {
    message.error('润色建议生成失败')
  } finally {
    polishLoading.value = false
    instruction.value = ''
  }
}

function applyPolishResult() {
  if (!props.editor || !selectedRange.value || !polishResult.value) return
  let nextText = polishResult.value.rewritten ?? ''

  if (!nextText && polishResult.value.basicAnnotations?.length) {
    nextText = selectedText.value
    polishResult.value.basicAnnotations.forEach(item => {
      nextText = nextText.replace(item.original, item.suggested)
    })
  }

  if (!nextText) return
  props.editor.commands.insertContentAt(selectedRange.value, nextText)
  polishResult.value = null
  selectedRange.value = null
  message.success('润色建议已应用')
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
              <NIcon :component="Wand2" :size="14" />
            </template>
            辅助
          </NButton>
        </template>

        <div class="ai-options">
          <div class="ai-options-title">创作辅助</div>
          <NInput
            v-model:value="instruction"
            size="small"
            placeholder="写作指示或风格要求（可选）"
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
            <NButton size="small" @click="startPolish('basic')">
              校正
            </NButton>
            <NButton size="small" @click="startPolish('advanced')">
              <template #icon>
                <NIcon :component="Paintbrush" :size="14" />
              </template>
              润色
            </NButton>
            <NButton size="small" @click="startPolish('style')">
              风格
            </NButton>
          </NSpace>
          <div class="ai-options-note">生成内容需手动接受后融入正文</div>
        </div>
      </NPopover>

      <NSpin v-if="isStreaming" size="small" class="ai-loading" />
      <NSpin v-if="polishLoading" size="small" class="ai-loading" />
    </div>

    <div v-if="hasPendingAiContent && !isStreaming" class="ai-accept-bar">
      <NIcon :component="Wand2" :size="14" class="ai-accept-icon" />
      <span class="ai-accept-text">辅助内容已生成</span>
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

    <div v-if="polishResult" class="polish-result-card">
      <div class="polish-result-head">
        <strong>润色建议</strong>
        <button @click="polishResult = null">×</button>
      </div>
      <div v-if="polishResult.rewritten" class="polish-rewritten">
        {{ polishResult.rewritten }}
      </div>
      <div v-else class="polish-annotations">
        <div
          v-for="(item, index) in polishResult.basicAnnotations ?? []"
          :key="`${item.original}-${index}`"
          class="polish-annotation"
        >
          <span>{{ item.original }}</span>
          <strong>{{ item.suggested }}</strong>
          <small>{{ item.reason }}</small>
        </div>
      </div>
      <div class="polish-result-actions">
        <NButton size="small" type="primary" @click="applyPolishResult">
          <template #icon>
            <NIcon :component="Check" :size="14" />
          </template>
          应用建议
        </NButton>
        <NButton size="small" @click="polishResult = null">放弃</NButton>
      </div>
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

.polish-result-card {
  width: min(380px, calc(100vw - 24px));
  background: var(--w-bg-elevated);
  border: 1px solid var(--w-annotation-border);
  border-radius: var(--w-radius-md);
  box-shadow: var(--w-shadow-md);
  overflow: hidden;
}

.polish-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--w-border-subtle);
}

.polish-result-head strong {
  font-size: var(--w-text-sm);
}

.polish-result-head button {
  width: 24px;
  height: 24px;
  color: var(--w-text-tertiary);
  border-radius: var(--w-radius-sm);
}

.polish-result-head button:hover {
  color: var(--w-text);
  background: var(--w-bg-hover);
}

.polish-rewritten {
  max-height: 220px;
  overflow-y: auto;
  padding: 12px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-sm);
  line-height: 1.7;
  white-space: pre-wrap;
}

.polish-annotations {
  max-height: 220px;
  overflow-y: auto;
  padding: 8px;
}

.polish-annotation {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 6px;
  padding: 8px;
  border-bottom: 1px solid var(--w-border-subtle);
  font-size: var(--w-text-xs);
}

.polish-annotation span {
  color: var(--w-danger);
}

.polish-annotation strong {
  color: var(--w-success);
  font-weight: 600;
}

.polish-annotation small {
  grid-column: 1 / -1;
  color: var(--w-text-tertiary);
  line-height: 1.5;
}

.polish-result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--w-border-subtle);
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
