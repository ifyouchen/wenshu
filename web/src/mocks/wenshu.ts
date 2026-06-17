import type { StyleTemplateInfo } from '@/api/styleTemplate'
import type { ScriptSceneInfo } from '@/api/types'

export interface PlotNode {
  id: string
  title: string
  summary: string
  status: 'todo' | 'writing' | 'done'
}

export interface SceneNote {
  sceneId: string
  text: string
}

export const fallbackStyleTemplates: StyleTemplateInfo[] = [
  {
    id: 'mock-template-fast',
    name: '短剧强钩子',
    type: 'short_drama',
    description: '更快建立冲突，段落短，台词推进明显。',
    sampleText: '她推开门时，所有人的目光都落在那份遗嘱上。',
    prompt: '强化短剧节奏、冲突与台词张力。',
    active: true,
  },
  {
    id: 'mock-template-web',
    name: '网文爽点推进',
    type: 'web_novel',
    description: '强调期待感、反转和章节末尾留钩。',
    sampleText: '众人还在嘲笑，他却已经拿出了第二张底牌。',
    prompt: '保持网文阅读快感，增强情绪反馈和章末悬念。',
    active: true,
  },
]

export function buildPlotNodes(outline?: string | null): PlotNode[] {
  const parts = (outline || '')
    .split(/\n+|[。；;]/)
    .map((item) => item.trim())
    .filter(Boolean)

  const source = parts.length
    ? parts
    : ['开场建立人物目标', '制造阻碍并升级矛盾', '用反转推动下一章期待']

  return source.slice(0, 6).map((summary, index) => ({
    id: `plot-${index + 1}`,
    title: `情节节点 ${index + 1}`,
    summary,
    status: index === 0 ? 'writing' : 'todo',
  }))
}

export function createMockScene(draftId: string, index: number): ScriptSceneInfo {
  return {
    id: `mock-scene-${Date.now()}-${index}`,
    draftId,
    sceneIndex: index,
    location: '待定场景',
    timeDesc: '日',
    content: '角色进入场景，冲突尚待补写。',
    sourceContent: '此场景由前端临时创建，刷新后不保证持久化。',
    version: 0,
    updatedAt: new Date().toISOString(),
  }
}

export const defaultSceneNotes: SceneNote[] = []

export const relationshipHints = [
  '核心关系先按人物动机梳理，后端关系图接口补齐后可替换为真实数据。',
  '锁定角色不会参与 AI 自动改写，但仍可作为上下文参考。',
]
