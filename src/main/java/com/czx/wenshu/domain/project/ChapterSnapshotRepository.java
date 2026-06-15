package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterSnapshotRepository {

    ChapterSnapshot save(ChapterSnapshot snapshot);

    List<ChapterSnapshot> findByChapterId(UUID chapterId);

    Optional<ChapterSnapshot> findById(UUID id);

    void deleteById(UUID id);
}