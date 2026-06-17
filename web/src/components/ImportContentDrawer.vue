<script setup lang="ts">
/**
 * 内容导入抽屉：上传文件解析预览或粘贴文本导入。
 */
import { computed, ref, watch } from 'vue'
import {
  NAlert,
  NButton,
  NDrawer,
  NDrawerContent,
  NIcon,
  NInput,
  NSelect,
  NSpace,
  NSpin,
  useMessage,
} from 'naive-ui'
import { FileText, FileUp, Send } from 'lucide-vue-next'
import type { OutlineInfo } from '@/api/project'
import { applyImport, parseImportFile, pasteImport, type ImportPreviewInfo } from '@/api/import'

const props = defineProps<{
  show: boolean
  projectId: string
  outline: OutlineInfo | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  imported: []
}>()

const message = useMessage()

const mode = ref<'file' | 'paste'>('file')
const volumeId = ref('')
const selectedFile = ref<File | null>(null)
const pasteText = ref('')
const preview = ref<ImportPreviewInfo | null>(null)
const parsing = ref(false)
const applying = ref(false)

const volumeOptions = computed(() =>
  (props.outline?.volumes ?? []).map(volume => ({
    label: volume.title || '未命名卷',
    value: volume.id,
  })),
)

watch(() => props.show, (visible) => {
  if (visible && !volumeId.value && volumeOptions.value.length) {
    volumeId.value = volumeOptions.value[0].value
  }
})

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  preview.value = null
}

async function handleParseFile() {
  if (!selectedFile.value) {
    message.warning('请先选择 TXT 或 DOCX 文件')
    return
  }
  parsing.value = true
  try {
    const res = await parseImportFile(props.projectId, selectedFile.value)
    preview.value = res.data.data
    message.success(`已识别 ${preview.value.totalChapters} 个章节`)
  } catch {
    message.error('文件解析失败，请检查格式或稍后重试')
  } finally {
    parsing.value = false
  }
}

async function handleApply() {
  if (!volumeId.value) {
    message.warning('请选择导入到哪个卷')
    return
  }
  applying.value = true
  try {
    if (mode.value === 'paste') {
      if (!pasteText.value.trim()) {
        message.warning('请先粘贴正文内容')
        return
      }
      await pasteImport(props.projectId, volumeId.value, pasteText.value)
    } else {
      if (!preview.value) {
        await handleParseFile()
        if (!preview.value) return
      }
      await applyImport(preview.value.parseId, volumeId.value)
    }
    message.success('内容已导入')
    emit('imported')
    emit('update:show', false)
    selectedFile.value = null
    pasteText.value = ''
    preview.value = null
  } catch {
    message.error('导入失败，请稍后重试')
  } finally {
    applying.value = false
  }
}
</script>

<template>
  <NDrawer
    :show="show"
    width="420"
    placement="right"
    @update:show="emit('update:show', $event)"
  >
    <NDrawerContent title="导入稿件" :native-scrollbar="false">
      <div class="import-drawer">
        <NAlert type="default" class="import-alert">
          支持 TXT / DOCX 文件解析，也可以直接粘贴正文。导入前请先选择目标卷。
        </NAlert>

        <label class="field-label">目标卷</label>
        <NSelect
          v-model:value="volumeId"
          :options="volumeOptions"
          placeholder="请选择卷"
          class="field-control"
        />

        <div class="mode-tabs">
          <button :class="{ active: mode === 'file' }" @click="mode = 'file'">
            <NIcon :component="FileUp" :size="15" />
            文件导入
          </button>
          <button :class="{ active: mode === 'paste' }" @click="mode = 'paste'">
            <NIcon :component="FileText" :size="15" />
            粘贴导入
          </button>
        </div>

        <template v-if="mode === 'file'">
          <label class="file-drop">
            <input type="file" accept=".txt,.docx" @change="onFileChange" />
            <NIcon :component="FileUp" :size="24" />
            <strong>{{ selectedFile?.name || '选择 TXT / DOCX 文件' }}</strong>
            <span>单文件请控制在后端限制范围内</span>
          </label>

          <NButton
            block
            secondary
            :disabled="!selectedFile"
            :loading="parsing"
            @click="handleParseFile"
          >
            解析章节预览
          </NButton>

          <NSpin :show="parsing">
            <div v-if="preview" class="preview-list">
              <div class="preview-summary">
                共识别 {{ preview.totalChapters }} 章
              </div>
              <div v-for="chapter in preview.chapters.slice(0, 8)" :key="chapter.index" class="preview-item">
                <strong>{{ chapter.title || `第 ${chapter.index + 1} 章` }}</strong>
                <small>{{ chapter.wordCount }} 字</small>
                <p>{{ chapter.contentPreview }}</p>
              </div>
              <div v-if="preview.chapters.length > 8" class="preview-more">
                还有 {{ preview.chapters.length - 8 }} 章将在确认后导入
              </div>
            </div>
          </NSpin>
        </template>

        <template v-else>
          <NInput
            v-model:value="pasteText"
            type="textarea"
            placeholder="粘贴已有正文，系统会按章节标题自动切分。"
            :rows="12"
            class="paste-input"
          />
          <div class="paste-count">{{ pasteText.length.toLocaleString() }} 字符</div>
        </template>
      </div>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="emit('update:show', false)">取消</NButton>
          <NButton type="primary" :loading="applying" @click="handleApply">
            <template #icon>
              <NIcon :component="Send" :size="14" />
            </template>
            确认导入
          </NButton>
        </NSpace>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.import-drawer {
  display: flex;
  flex-direction: column;
  gap: var(--w-space-3);
}

.import-alert {
  margin-bottom: var(--w-space-1);
}

.field-label {
  font-size: var(--w-text-xs);
  font-weight: 600;
  color: var(--w-text-tertiary);
}

.field-control {
  margin-top: -6px;
}

.mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--w-space-2);
}

.mode-tabs button {
  height: 38px;
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-sm);
  color: var(--w-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all var(--w-transition-base);
}

.mode-tabs button.active,
.mode-tabs button:hover {
  color: var(--w-text);
  background: var(--w-brand-soft);
  border-color: var(--w-brand);
}

.file-drop {
  min-height: 150px;
  border: 1px dashed var(--w-border-strong);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-tertiary);
  color: var(--w-text-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: var(--w-space-4);
  text-align: center;
  cursor: pointer;
}

.file-drop input {
  display: none;
}

.file-drop strong {
  max-width: 100%;
  color: var(--w-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-drop span,
.paste-count,
.preview-more {
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
}

.preview-list {
  border: 1px solid var(--w-border-default);
  border-radius: var(--w-radius-md);
  background: var(--w-bg-secondary);
  overflow: hidden;
}

.preview-summary {
  padding: 10px 12px;
  border-bottom: 1px solid var(--w-border-subtle);
  font-size: var(--w-text-sm);
  font-weight: 600;
}

.preview-item {
  padding: 10px 12px;
  border-bottom: 1px solid var(--w-border-subtle);
}

.preview-item strong {
  display: block;
  font-size: var(--w-text-sm);
}

.preview-item small {
  color: var(--w-text-tertiary);
  font-size: var(--w-text-xs);
}

.preview-item p {
  margin-top: 4px;
  color: var(--w-text-secondary);
  font-size: var(--w-text-xs);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-more {
  padding: 10px 12px;
}

.paste-input {
  background: var(--w-bg-paper) !important;
}

.paste-count {
  text-align: right;
}
</style>
