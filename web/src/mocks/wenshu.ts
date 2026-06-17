import type { StyleTemplateInfo } from '@/api/styleTemplate'
import type { ProjectInfo, ScriptDraftInfo, ScriptSceneInfo } from '@/api/types'
import type { CharacterInfo, WorldElementInfo } from '@/api/character'
import type { ChapterInfo, OutlineInfo } from '@/api/project'

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

const now = new Date().toISOString()

export const demoProject: ProjectInfo = {
  id: 'demo-project',
  userId: 'demo-user',
  title: '长夜将明',
  genre: '悬疑短剧',
  synopsis: '落魄编剧卷入旧案，用一部未播短剧反向追查真相。',
  worldview: '近未来影视城，AI 审稿系统与真实案件线索交织。',
  totalWords: 12800,
  dailyCharGoal: 2000,
  status: 'draft',
  createdAt: now,
  updatedAt: now,
}

export const demoProjects: ProjectInfo[] = [demoProject]

export const demoCharacters: CharacterInfo[] = [
  {
    id: 'demo-character-1',
    projectId: demoProject.id,
    name: '林照',
    role: '主角',
    appearance: '常穿黑色夹克，眼下有长期失眠的青影。',
    personality: '克制、敏感，面对线索时近乎固执。',
    abilities: '["剧本结构","观察细节","临场应变"]',
    speechStyle: '短句多，习惯反问。',
    status: '{}',
    locked: false,
    createdAt: now,
    updatedAt: now,
  },
  {
    id: 'demo-character-2',
    projectId: demoProject.id,
    name: '许棠',
    role: '搭档',
    appearance: '白衬衫、银框眼镜，随身带录音笔。',
    personality: '锋利、理性，但对旧案有隐秘愧疚。',
    abilities: '["资料检索","谈判","媒体资源"]',
    speechStyle: '信息密度高，几乎不说废话。',
    status: '{}',
    locked: true,
    createdAt: now,
    updatedAt: now,
  },
]

export const demoWorldElements: WorldElementInfo[] = [
  {
    id: 'demo-world-1',
    projectId: demoProject.id,
    type: 'dict',
    name: '灰档案',
    description: '平台标记为不可播但未删除的剧本与影像资料。',
    aliases: ['灰库', '禁档'],
    locked: false,
    createdAt: now,
  },
  {
    id: 'demo-world-2',
    projectId: demoProject.id,
    type: 'world',
    name: '西桥影视城',
    description: '旧摄影棚改造的创作园区，案发现场与主角工作地重合。',
    aliases: ['西桥'],
    locked: false,
    createdAt: now,
  },
]

export const demoChapter: ChapterInfo = {
  id: 'demo-chapter-1',
  volumeId: 'demo-volume-1',
  projectId: demoProject.id,
  title: '第一章 灰档案',
  outline: '林照收到匿名剧本。许棠发现剧本细节对应五年前旧案。二人决定进入西桥影视城查证。',
  content: '<p>凌晨两点，林照的邮箱里多了一份没有署名的剧本。</p><p>标题只有三个字：灰档案。</p>',
  wordCount: 56,
  sortOrder: 1,
  status: 'draft',
  createdAt: now,
  updatedAt: now,
}

export function demoOutline(_projectId = demoProject.id): OutlineInfo {
  return {
    volumes: [
      {
        id: 'demo-volume-1',
        title: '第一卷 旧案重映',
        conflict: '主角必须判断剧本是创作恶作剧，还是凶手留下的第二次预告。',
        sortOrder: 1,
        chapters: [
          {
            id: demoChapter.id,
            title: demoChapter.title,
            outline: demoChapter.outline,
            wordCount: demoChapter.wordCount,
            status: demoChapter.status,
          },
          {
            id: 'demo-chapter-2',
            title: '第二章 失焦镜头',
            outline: '一段废弃样片暴露新的目击者。',
            wordCount: 0,
            status: 'pending',
          },
        ],
      },
    ].map((volume) => ({
      ...volume,
      chapters: volume.chapters.map((chapter) => ({ ...chapter })),
    })),
  }
}

export function demoChapterById(chapterId: string): ChapterInfo {
  if (chapterId === demoChapter.id) return { ...demoChapter }
  return {
    ...demoChapter,
    id: chapterId,
    title: '第二章 失焦镜头',
    content: '<p>废弃样片里的镜头抖了一下，画面边缘出现了本不该在场的人。</p>',
    outline: '一段废弃样片暴露新的目击者。',
    wordCount: 34,
  }
}

export const demoDraft: ScriptDraftInfo = {
  id: 'demo-draft',
  projectId: demoProject.id,
  title: '长夜将明 改编草稿',
  strategy: 'action',
  status: 'draft',
  totalScenes: 2,
  createdAt: now,
  updatedAt: now,
}

export const demoScenes: ScriptSceneInfo[] = [
  {
    id: 'demo-scene-1',
    draftId: demoDraft.id,
    sceneIndex: 0,
    location: '林照工作室',
    timeDesc: '夜',
    content: '内景 林照工作室 夜\n\n林照盯着屏幕，邮箱提示音响起。\n\n林照：这个时间，谁还发剧本？',
    sourceContent: '凌晨两点，林照的邮箱里多了一份没有署名的剧本。',
    version: 0,
    updatedAt: now,
  },
  {
    id: 'demo-scene-2',
    draftId: demoDraft.id,
    sceneIndex: 1,
    location: '西桥影视城',
    timeDesc: '晨',
    content: '外景 西桥影视城 晨\n\n许棠把录音笔放进口袋，抬头看向封闭的摄影棚。',
    sourceContent: '许棠发现剧本细节对应五年前旧案。',
    version: 0,
    updatedAt: now,
  },
]
