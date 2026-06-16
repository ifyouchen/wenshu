package com.czx.wenshu.application.stats;

import java.util.List;

/** 月度写作摘要（P4-08）。 */
public record MonthlySummaryInfo(
        String yearMonth,
        int totalChars,
        int activeDays,
        int avgCharsPerActiveDay,
        List<HeatmapEntry> dailyBreakdown) {
}
