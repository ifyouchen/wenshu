package com.czx.wenshu.application.stats;

/** 单个作品写作进度（P4-08）。 */
public record ProjectProgressInfo(
        String projectId,
        String title,
        int totalWords,
        int dailyGoal,
        int todayChars) {
}
