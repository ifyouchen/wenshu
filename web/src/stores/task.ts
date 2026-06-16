/**
 * 异步任务轮询 Store（P8-03）。
 * 管理正在进行中的 AI 任务列表，每 2 秒轮询一次进度。
 * 任务完成/失败后自动停止轮询并触发回调。
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTaskProgress } from '@/api/user'
import type { TaskProgress } from '@/api/types'

/** 任务完成回调函数类型。 */
type TaskCallback = (task: TaskProgress) => void

interface TrackedTask {
  taskId: string
  onComplete?: TaskCallback
  onFail?: TaskCallback
  intervalId?: ReturnType<typeof setInterval>
}

export const useTaskStore = defineStore('task', () => {
  /** 当前正在轮询的任务 Map（taskId → 任务详情）。 */
  const tasks = ref<Map<string, TaskProgress>>(new Map())
  /** 内部轮询定时器 Map。 */
  const trackers = ref<Map<string, TrackedTask>>(new Map())

  /**
   * 开始轮询指定任务的进度。
   *
   * @param taskId     任务 ID
   * @param onComplete 完成回调
   * @param onFail     失败回调
   */
  function track(taskId: string, onComplete?: TaskCallback, onFail?: TaskCallback): void {
    if (trackers.value.has(taskId)) return // 已在轮询中

    const tracker: TrackedTask = { taskId, onComplete, onFail }

    const poll = async () => {
      try {
        const res = await getTaskProgress(taskId)
        const progress = res.data.data
        tasks.value.set(taskId, progress)

        if (progress.status === 'completed') {
          stopTracking(taskId)
          onComplete?.(progress)
        } else if (progress.status === 'failed') {
          stopTracking(taskId)
          onFail?.(progress)
        }
      } catch {
        // 轮询失败不停止，继续尝试（网络抖动可恢复）
      }
    }

    poll() // 立即执行一次
    tracker.intervalId = setInterval(poll, 2_000)
    trackers.value.set(taskId, tracker)
  }

  /**
   * 停止指定任务的轮询并清除记录。
   *
   * @param taskId 任务 ID
   */
  function stopTracking(taskId: string): void {
    const tracker = trackers.value.get(taskId)
    if (tracker?.intervalId !== undefined) {
      clearInterval(tracker.intervalId)
    }
    trackers.value.delete(taskId)
  }

  /**
   * 获取指定任务的最新进度（从本地缓存读取，不发请求）。
   *
   * @param taskId 任务 ID
   */
  function getProgress(taskId: string): TaskProgress | undefined {
    return tasks.value.get(taskId)
  }

  /** 是否存在正在运行中的任务。 */
  function hasRunningTasks(): boolean {
    for (const task of tasks.value.values()) {
      if (task.status === 'pending' || task.status === 'running') return true
    }
    return false
  }

  return { tasks, track, stopTracking, getProgress, hasRunningTasks }
})
