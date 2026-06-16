package com.czx.wenshu.domain.project;

import java.util.Optional;
import java.util.UUID;

public interface ChapterSummaryRepository {

    /** 保存摘要（按 chapter_id UNIQUE 做 upsert）。 */
    void save(ChapterSummary summary);

    Optional<ChapterSummary> findByChapterId(UUID chapterId);

    void deleteByChapterId(UUID chapterId);
}
