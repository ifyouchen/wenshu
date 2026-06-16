package com.czx.wenshu.application.search;

import java.util.List;

/** 单章节内的搜索命中汇总。 */
public record ChapterSearchResult(String chapterId, String chapterTitle,
                                   int matchCount, List<MatchContext> matches) {
}
