package com.czx.wenshu.application.stats;

/**
 * 写作时间热力图条目（weekday × hour 维度，P0-1 修复）。
 *
 * @param weekday   星期几（0=周日，1=周一，...，6=周六）
 * @param hour      小时（0-23）
 * @param totalChars 该时段累计写作字数
 */
public record TimeHeatmapEntry(int weekday, int hour, long totalChars) {
}
