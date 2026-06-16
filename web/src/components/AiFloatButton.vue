<script setup lang="ts">
/**
 * AI 浮窗（P8-08 / P8-17）。
 *
 * P8-08：文本选中时显示，提供续写/润色入口；
 *        SSE 续写使用 fetch ReadableStream + RAF 批量写入 TipTap，
 *        AI 生成内容带绿色左边框标识。
 *
 * P8-17：异常状态 UI 集成：
 *        - A 级：AI 服务不可用（503 / 网络错误）
 *        - B 级：结果不可用（生成失败、超时）
 *        - C 级：配额不足（429 / RATE_LIMITED）
 *        - D 级：内容被替换（内容安全过滤）
 */
import { ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { NButton, NPopover, NSpace, NInput, NSpin, NText, useMessage } from 'naive-ui'
import { getAccessToken } from '@/api/client'
import ErrorStateAlert from '@/components/ErrorStateAlert.vue'
import { acceptAiContent } from '@/api/project'

const props = defineProps<{
  editor: Editor | undefined
  chapterId: string | undefined
  /** AI 浮窗是否可见（由父组件根据文本选中状态控制）。 */
  visible: boolean
}>()

const emit = defineEmits<{
  /** SSE 开始流式生成。 */
  streamStart: []
  /** SSE 流式生成完成。 */
  streamEnd: []
}>()

const message = useMessage()
const isStreaming = ref(false)
const showOptions = ref(false)
const instruction = ref('')

/** AI 内容已插入，等待用户接受或放弃（P0-1）。 */
const aiContentInserted = ref(false)
/** 是否有待处理的 AI 内容（流式生成完成后显示接受/放弃按钮）。 */
const hasPendingAiContent = ref(false)

// P8-17：异常状态
const errorLevel = ref<'A' | 'B' | 'C' | 'D' | null>(null)
const errorMessage = ref('')
const blockedContent = ref('')

/**
 * 根据错误信息推断异常级别（P8-17）。
 * - C 级：配额不足（含 "配额" / "额度" / "RATE_LIMITED" / 429）
 * - D 级：内容安全过滤（含 "安全" / "替换" / "违规"）
 * - A 级：服务不可用（503 / "service" / "不可用"）
 * - B 级：其他生成失败
 */
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

/** 清除当前错误状态。 */
function clearError() {
  errorLevel.value = null
  errorMessage.value = ''
  blockedContent.value = ''
}

/**
 * 触发 SSE 续写（P8-08 核心）。
 * 使用 fetch + ReadableStream，通过 RAF 批量写入 TipTap。
 * AI 生成内容插入时带 data-ai 属性作为绿色左边框标识。
 */
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

  // 在选区末尾或文档末尾插入
  const { to } = props.editor.state.selection
  props.editor.commands.focus()
  props.editor.commands.setTextSelection(to)

  // 用于标记 AI 段落开始
  props.editor.commands.insertContent('\n')

  // RAF 批量写入缓冲区
  const tokenBuffer: string[] = []
  let rafId: number | null = null

  function flushTokens() {
    if (!tokenBuffer.length) return
    const chunk = tokenBuffer.splice(0).join('')
    // 以 data-ai 属性标识 AI 内容
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
      // P8-17：HTTP 错误级别推断
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
          // 第一个 token 到达时标记 AI 内容已开始插入
          if (!aiContentInserted.value) aiContentInserted.value = true
          tokenBuffer.push(data)
          scheduleFlush()
        } else if (lastEventType === 'done') {
          break
        } else if (lastEventType === 'error' || lastEventType === 'timeout') {
          // P8-17：SSE 错误级别推断
          const level = inferErrorLevel(data)
          errorLevel.value = level
          errorMessage.value = data
          if (level === 'D') blockedContent.value = data
          break
        }
        lastEventType = 'token'
      }
    }

    // 最后刷新一次残余 buffer
    if (rafId) cancelAnimationFrame(rafId)
    flushTokens()

  } catch (err: unknown) {
    // P8-17：网络错误 → A 级（服务不可用）
    const errMsg = (err as Error).message || 'AI 续写请求失败'
    errorLevel.value = 'A'
    errorMessage.value = errMsg
  } finally {
    isStreaming.value = false
    emit('streamEnd')
    // 若有 AI 内容插入且无错误，显示接受/放弃按钮（P0-1）
    if (aiContentInserted.value && !errorLevel.value) {
      hasPendingAiContent.value = true
    }
    instruction.value = ''
  }
}

/**
 * 接受 AI 生成内容（P0-1）。
 * 统计 span[data-ai="true"] 的字符数，调用后端 accept-ai 接口，
 * 然后移除所有 data-ai 属性标记（保留文本）。
 */
async function handleAcceptAi() {
  if (!props.editor || !props.chapterId) return

  // 统计 AI 内容字符数
  const editorDom = props.editor.view.dom as HTMLElement
  const aiSpans = editorDom.querySelectorAll('span[data-ai="true"]')
  let acceptedCharsCount = 0
  aiSpans.forEach(span => {
    acceptedCharsCount += (span.textContent ?? '').length
  })

  // 获取当前编辑器纯文本内容
  const editorContent = props.editor.getHTML()

  try {
    await acceptAiContent(props.chapterId, acceptedCharsCount, editorContent)
    message.success('已接受 AI 内容，快照已创建')
  } catch {
    message.warning('接受 AI 内容时记录失败，但内容已保留')
  }

  // 移除 data-ai 属性（保留文本，取消绿色标记）
  editorDom.querySelectorAll('span[data-ai]').forEach(span => {
    span.removeAttribute('data-ai')
  })

  hasPendingAiContent.value = false
  aiContentInserted.value = false
}

/**
 * 放弃 AI 生成内容（P0-1）。
 * 删除编辑器中所有带 data-ai="true" 的 span 元素及其文本，然后同步编辑器状态。
 */
function handleRejectAi() {
  if (!props.editor) return

  // 直接通过 DOM 删除 AI 内容 span 及其文本
  const editorDom = props.editor.view.dom as HTMLElement
  const aiSpans = editorDom.querySelectorAll('span[data-ai="true"]')
  aiSpans.forEach(span => {
    span.remove()
  })

  // 同步编辑器状态（从 DOM 重新解析）
  const newContent = editorDom.innerHTML
  props.editor.commands.setContent(newContent)

  message.info('已放弃 AI 内容')
  hasPendingAiContent.value = false
  aiContentInserted.value = false
}

/** 润色当前选中文本（跳转到单独的润色 API）。 */
async function startPolish() {
  message.info('润色功能（P8-08 增强实现）')
}
</script>

<template>
  <div class="ai-float-root">
    <!-- P8-17：异常状态展示（A/B/C/D 级）-->
    <ErrorStateAlert
      v-if="errorLevel"
      :level="errorLevel"
      :message="errorMessage"
      :blocked-content="blockedContent"
      style="position: absolute; bottom: 48px; right: 0; z-index: 200; width: 320px"
      @close="clearError"
      @upgrade="$router.push('/settings?tab=sub')"
    />

    <!-- AI 主按钮（visible 或 streaming 时显示）-->
    <div v-if="visible || isStreaming">
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
            style="box-shadow: 0 2px 8px rgba(0,0,0,0.2)"
          >
            ✨ AI
          </NButton>
        </template>

        <NSpace vertical :size="8" style="width: 220px">
          <NText strong style="font-size: 13px">AI 写作辅助</NText>
          <NInput
            v-model:value="instruction"
            size="small"
            placeholder="写作指示（可选）"
            @keydown.enter="startContinue"
          />
          <!-- P2-2：预计消耗提示 -->
          <NText depth="3" style="font-size: 11px">预计消耗约 800-1500 字配额</NText>
          <NSpace :size="6">
            <NButton size="small" type="primary" @click="startContinue">
              续写
            </NButton>
            <NButton size="small" @click="startPolish">润色</NButton>
          </NSpace>
          <NText depth="3" style="font-size: 11px">AI 内容需手动接受后计入配额</NText>
        </NSpace>
      </NPopover>

      <NSpin v-if="isStreaming" size="small" style="margin-left: 8px" />
    </div>

    <!-- P0-1：AI 内容接受/放弃按钮（流式完成后显示）-->
    <div v-if="hasPendingAiContent && !isStreaming" class="ai-accept-bar">
      <NText depth="3" style="font-size: 12px; margin-right: 8px">AI 内容已生成</NText>
      <NButton size="small" type="primary" @click="handleAcceptAi">接受</NButton>
      <NButton size="small" style="margin-left: 6px" @click="handleRejectAi">放弃</NButton>
    </div>
  </div>
</template>

<style scoped>
.ai-float-root {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  position: sticky;
  bottom: 16px;
  right: 16px;
  float: right;
  z-index: 100;
  margin: 8px;
  /* P8-17：确保错误弹出层不被裁剪 */
  overflow: visible;
}

.ai-accept-bar {
  display: flex;
  align-items: center;
  margin-top: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 6px 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}
</style>
