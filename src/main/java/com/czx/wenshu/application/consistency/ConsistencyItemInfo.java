package com.czx.wenshu.application.consistency;

import com.czx.wenshu.domain.consistency.ConsistencyReportItem;

/**
 * 一致性审查条目响应 DTO（P6-06/P6-07）。
 */
public record ConsistencyItemInfo(
        String id,
        String type,
        String character,
        String chapterHint,
        String description,
        String suggestion,
        String status,
        String createdAt,
        String updatedAt) {

    /**
     * 从领域对象构建 DTO。
     *
     * @param item 审查条目领域对象
     */
    public static ConsistencyItemInfo from(ConsistencyReportItem item) {
        return new ConsistencyItemInfo(
                item.id().toString(),
                item.type(),
                item.character(),
                item.chapterHint(),
                item.description(),
                item.suggestion(),
                item.status(),
                item.createdAt().toString(),
                item.updatedAt().toString());
    }
}
