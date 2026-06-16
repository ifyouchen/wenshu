package com.czx.wenshu.application.stats;

/** 单日写作字数，用于热力图（P4-08）。 */
public record HeatmapEntry(String date, int chars) {
}
