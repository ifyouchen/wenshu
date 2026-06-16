package com.czx.wenshu.application.stats;

/** 单日写作数据，用于趋势折线图（P4-07）。 */
public record DailyStatEntry(String date, int manualChars, int totalChars) {
}
