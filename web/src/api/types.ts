/** 统一 API 响应包装（对应后端 Result<T>）。 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** 用户信息。 */
export interface UserInfo {
  id: string
  email: string
  nickname: string | null
  avatarUrl: string | null
  identityType: string
  isEmailVerified: boolean
  aiTrainConsent: boolean
  dailyCharGoal: number
  createdAt: string
  updatedAt: string
}

/** 令牌对（登录/注册/刷新返回）。 */
export interface TokenPair {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  refreshExpiresIn: number
}

/** 配额详情。 */
export interface QuotaInfo {
  yearMonth: string
  usedChars: number
  limitChars: number
  usedAdaptations: number
  limitAdaptations: number
  remainingChars: number
  remainingAdaptations: number
}

/** 异步任务进度。 */
export interface TaskProgress {
  taskId: string
  projectId: string | null
  taskType: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  currentStep: number
  totalSteps: number | null
  stepLabel: string | null
  progressPct: number
  resultId: string | null
  resultJson: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

/** 作品信息。 */
export interface ProjectInfo {
  id: string
  userId: string
  title: string
  genre: string | null
  synopsis: string | null
  worldview: string | null
  totalWords: number
  dailyCharGoal: number
  status: string
  createdAt: string
  updatedAt: string
}

/** 剧本草稿。 */
export interface ScriptDraftInfo {
  id: string
  projectId: string
  title: string | null
  strategy: string | null
  status: string
  totalScenes: number | null
  createdAt: string
  updatedAt: string
}

/** 剧本场景。 */
export interface ScriptSceneInfo {
  id: string
  draftId: string
  sceneIndex: number
  location: string | null
  timeDesc: string | null
  content: string | null
  sourceContent: string | null
  version: number
  updatedAt: string
}
