package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Chapter;

public record ChapterInfo(String id, String volumeId, String projectId, String title, String outline,
                           String content, int wordCount, int sortOrder, String status, String createdAt, String updatedAt) {

    public static ChapterInfo from(Chapter c) {
        return new ChapterInfo(c.id().toString(), c.volumeId().toString(), c.projectId().toString(), c.title(),
                c.outline(), c.content(), c.wordCount(), c.sortOrder(), c.status().value(),
                c.createdAt().toString(), c.updatedAt().toString());
    }

    public static ChapterInfo fromWithoutContent(Chapter c) {
        return new ChapterInfo(c.id().toString(), c.volumeId().toString(), c.projectId().toString(), c.title(),
                c.outline(), null, c.wordCount(), c.sortOrder(), c.status().value(),
                c.createdAt().toString(), c.updatedAt().toString());
    }
}