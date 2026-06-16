/**
 * 剧本草稿 Store（P8-03）。
 * 管理当前打开的草稿、场景分页、场景编辑状态。
 * 与 TaskStore 联动：改编任务完成后自动刷新草稿状态。
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getDraft, listScenes, updateScene, convertScript, exportDraft } from '@/api/script'
import { useTaskStore } from './task'
import type { ScriptDraftInfo, ScriptSceneInfo } from '@/api/types'

export const useScriptDraftStore = defineStore('scriptDraft', () => {
  /** 当前打开的草稿。 */
  const draft = ref<ScriptDraftInfo | null>(null)
  /** 当前页场景列表。 */
  const scenes = ref<ScriptSceneInfo[]>([])
  /** 场景总数。 */
  const totalScenes = ref(0)
  /** 当前页码（0-based）。 */
  const currentPage = ref(0)
  /** 每页场景数。 */
  const pageSize = ref(20)
  /** 是否正在加载。 */
  const loading = ref(false)

  /** 是否还有更多场景可加载。 */
  const hasMore = computed(
    () => (currentPage.value + 1) * pageSize.value < totalScenes.value,
  )

  /**
   * 加载指定草稿的详情。
   *
   * @param draftId 草稿 ID
   */
  async function loadDraft(draftId: string): Promise<void> {
    loading.value = true
    try {
      const res = await getDraft(draftId)
      draft.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载草稿的场景（分页，追加模式）。
   *
   * @param draftId 草稿 ID
   * @param reset   true 则从第 0 页重新加载，false 则追加下一页
   */
  async function loadScenes(draftId: string, reset = false): Promise<void> {
    if (reset) {
      currentPage.value = 0
      scenes.value = []
    }
    loading.value = true
    try {
      const res = await listScenes(draftId, currentPage.value, pageSize.value)
      const { total, scenes: newScenes } = res.data.data
      totalScenes.value = total
      scenes.value = reset ? newScenes : [...scenes.value, ...newScenes]
      if (!reset) currentPage.value++
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新场景内容（含乐观锁），成功后刷新本地场景列表。
   *
   * @param sceneId 场景 ID
   * @param data    更新数据（包含 version）
   */
  async function saveScene(
    sceneId: string,
    data: { content?: string; location?: string; timeDesc?: string; version: number },
  ): Promise<ScriptSceneInfo> {
    const res = await updateScene(sceneId, data)
    const updated = res.data.data
    const idx = scenes.value.findIndex((s) => s.id === sceneId)
    if (idx !== -1) scenes.value[idx] = updated
    return updated
  }

  /**
   * 提交改编任务，并自动轮询进度。
   * 任务完成后刷新草稿状态。
   *
   * @param projectId 作品 ID
   * @param params    改编参数
   */
  async function submitConversion(
    projectId: string,
    params: { title?: string; psychologyStrategy?: string },
  ): Promise<{ taskId: string; draftId: string }> {
    const res = await convertScript({ projectId, ...params })
    const { taskId, draftId } = res.data.data

    // 任务完成后自动刷新草稿状态
    const taskStore = useTaskStore()
    taskStore.track(taskId, async () => {
      await loadDraft(draftId)
      await loadScenes(draftId, true)
    })

    return { taskId, draftId }
  }

  /**
   * 提交导出任务。
   *
   * @param draftId 草稿 ID
   * @param format  导出格式
   */
  async function submitExport(
    draftId: string,
    format: 'docx' | 'fdx' | 'storyboard' = 'docx',
  ): Promise<{ taskId: string }> {
    const res = await exportDraft(draftId, format)
    return { taskId: res.data.data.taskId }
  }

  return {
    draft,
    scenes,
    totalScenes,
    currentPage,
    pageSize,
    loading,
    hasMore,
    loadDraft,
    loadScenes,
    saveScene,
    submitConversion,
    submitExport,
  }
})
