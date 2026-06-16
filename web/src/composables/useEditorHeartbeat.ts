/**
 * 编辑器多标签页心跳检测（P2-5）。
 * 当同一作品章节在其他标签页也打开时，顶部非阻断提示。
 *
 * 原理：每 3 秒向 localStorage 写入当前标签页的心跳信息（chapterId + 时间戳 + 随机 tab ID）。
 * 同时检测是否有其他标签页在 6 秒内写入了相同 chapterId 的心跳，若有则标记 otherTabEditing。
 */
import { ref, onMounted, onUnmounted } from 'vue'

const HEARTBEAT_KEY = 'wenshu:editor:active'
/** 心跳写入间隔（毫秒）。 */
const HEARTBEAT_INTERVAL = 3000
/** 超过此时间未更新心跳视为标签页已关闭（毫秒）。 */
const HEARTBEAT_EXPIRE = 6000

/** 当前标签页唯一 ID（随机生成，用于区分自身心跳）。 */
const TAB_ID = Math.random().toString(36).slice(2)

/**
 * 编辑器多标签页心跳检测 composable（P2-5）。
 * @param chapterId - 当前编辑的章节 ID
 * @returns `otherTabEditing` - 是否有其他标签页正在编辑同一章节
 */
export function useEditorHeartbeat(chapterId: string) {
  const otherTabEditing = ref(false)
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  /** 向 localStorage 写入当前标签页心跳。 */
  function writeHeartbeat() {
    localStorage.setItem(HEARTBEAT_KEY, JSON.stringify({
      chapterId,
      ts: Date.now(),
      tab: TAB_ID,
    }))
  }

  /** 检测是否有其他标签页正在编辑同一章节。 */
  function checkOtherTab() {
    const raw = localStorage.getItem(HEARTBEAT_KEY)
    if (!raw) return
    try {
      const data = JSON.parse(raw)
      // 排除自身心跳，且心跳未过期，且是同一章节
      if (
        data.tab !== TAB_ID &&
        data.chapterId === chapterId &&
        Date.now() - data.ts < HEARTBEAT_EXPIRE
      ) {
        otherTabEditing.value = true
      } else {
        otherTabEditing.value = false
      }
    } catch {
      otherTabEditing.value = false
    }
  }

  onMounted(() => {
    checkOtherTab()
    writeHeartbeat()
    heartbeatTimer = setInterval(() => {
      writeHeartbeat()
      checkOtherTab()
    }, HEARTBEAT_INTERVAL)
  })

  onUnmounted(() => {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    localStorage.removeItem(HEARTBEAT_KEY)
  })

  return { otherTabEditing }
}
