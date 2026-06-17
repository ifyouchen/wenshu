/**
 * 写作统计 API（P8-10）。
 * 覆盖写作总览、热力图、各作品进度、月度摘要。
 */
import client from './client'
import type {ApiResponse} from './types'

/** 写作统计总览（今日、7日趋势、连续天数）。 */
export interface WritingOverview {
  todayChars: number
  dailyGoal: number
  todayProgress: number
  streak: number
  totalChars: number
  trend: DailyStats[]
}

/** 单日统计。 */
export interface DailyStats {
  date: string
  /** 合计字数。 */
  chars: number
  /** 手动输入字数（P1-3）。 */
  manualChars?: number
  /** 辅助生成接受字数（P1-3）。 */
  aiAcceptedChars?: number
}

/** 热力图数据（365天）。 */
export interface HeatmapData {
  days: DailyStats[]
}

/** 各作品今日进度。 */
export interface ProjectProgress {
  projectId: string
  title: string
  totalWords: number
  dailyCharGoal: number
  todayChars: number
  progress: number
}

/** 月度摘要。 */
export interface MonthlySummary {
  yearMonth: string
  totalChars: number
  activeDays: number
  avgCharsPerDay: number
  days: DailyStats[]
}

/** 获取写作统计总览（今日、7日趋势、连续天数）。 */
export function getWritingOverview() {
  return client.get<ApiResponse<WritingOverview>>('/stats/writing')
}

/** 获取写作热力图（365天每日字数）。 */
export function getWritingHeatmap() {
  return client.get<ApiResponse<HeatmapData>>('/stats/writing/heatmap')
}

/** 获取各作品今日写作进度。 */
export function getProjectProgress() {
  return client.get<ApiResponse<ProjectProgress[]>>('/stats/writing/projects')
}

/** 获取月度摘要（yearMonth 格式：2026-06）。 */
export function getMonthlySummary(yearMonth: string) {
  return client.get<ApiResponse<MonthlySummary>>(`/stats/writing/monthly/${yearMonth}`)
}

/** 写作时段热力图条目（小时×星期，P1-1）。 */
export interface TimeHeatmapEntry {
  /** 星期几：0=周日...6=周六 */
  weekday: number
  /** 小时：0-23 */
  hour: number
  totalChars: number
}

/** 获取写作时段热力图（小时×星期，P1-1）。 */
export function getWritingTimeHeatmap() {
  return client.get<ApiResponse<TimeHeatmapEntry[]>>('/stats/writing/time-heatmap')
}

