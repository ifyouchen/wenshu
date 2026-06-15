package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.ChapterSnapshot;

public record SnapshotInfo(String id, String chapterId, int wordCount, String snapshotType,
                           String label, String createdAt) {

    public static SnapshotInfo from(ChapterSnapshot s) {
        return new SnapshotInfo(s.id().toString(), s.chapterId().toString(), s.wordCount(),
                s.snapshotType(), s.label(), s.createdAt().toString());
    }
}