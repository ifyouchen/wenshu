package com.czx.wenshu.application.stats;

import java.util.List;

/** 写作统计总览（P4-07）。 */
public record WritingOverviewInfo(
        int todayChars,
        int todayGoal,
        double todayProgress,
        int streak,
        long totalCharsAllTime,
        List<DailyStatEntry> trend) {
}
