package com.czx.wenshu.application.search;

/** 替换操作中受影响的单个章节汇总。 */
public record AffectedChapterResult(String chapterId, String chapterTitle,
                                     int replacedCount, String snapshotId) {
}
