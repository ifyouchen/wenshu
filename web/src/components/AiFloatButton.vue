<script setup lang="ts">
/**
 * AI 浮窗（P8-08）。
 * 文本选中时显示，提供续写/润色/分支入口。
 * SSE 续写使用 fetch ReadableStream + RAF 批量写入 TipTap，
 * AI 生成内容带绿色左边框标识。
 */
import { ref } from 'vue'
import type { Editor } from '@tiptap/vue-3'
import { NButton, NPopover, NSpace, NInput, NSpin, NText, useMessage } from 'naive-ui'
import { getAccessToken } from '@/api/client'

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
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let partial = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      partial += decoder.decode(value, { stream: true })
      const lines = partial.split('\n')
      partial = lines.pop() ?? ''

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (!data) continue

        // 解析 SSE 事件
        const prevLine = lines[lines.indexOf(line) - 1] ?? ''
        const eventType = prevLine.startsWith('event:') ? prevLine.slice(6).trim() : 'token'

        if (eventType === 'token') {
          tokenBuffer.push(data)
          scheduleFlush()
        } else if (eventType === 'done') {
          break
        } else if (eventType === 'error' || eventType === 'timeout') {
          message.error(`AI 生成失败：${data}`)
          break
        }
      }
    }

    // 最后刷新一次残余 buffer
    if (rafId) cancelAnimationFrame(rafId)
    flushTokens()

  } catch (err: unknown) {
    message.error((err as Error).message || 'AI 续写请求失败')
  } finally {
    isStreaming.value = false
    emit('streamEnd')
    instruction.value = ''
  }
}

/** 润色当前选中文本（跳转到单独的润色 API）。 */
async function startPolish() {
  message.info('润色功能（P8-08 增强实现）')
}
</script>

<template>
  <div v-if="visible || isStreaming" class="ai-float-root">
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
</template>

<style scoped>
.ai-float-root {
  display: flex;
  align-items: center;
  position: sticky;
  bottom: 16px;
  right: 16px;
  float: right;
  z-index: 100;
  margin: 8px;
}
</style>
