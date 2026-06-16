package com.czx.wenshu.application.search;

import java.util.List;

/** 全书搜索结果，按章节分组返回。 */
public record SearchResultInfo(int total, List<ChapterSearchResult> chapters) {
}
