package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.Volume;
import java.util.List;

public record OutlineInfo(List<VolumeNode> volumes) {

    public record VolumeNode(String id, String projectId, String title, String conflict,
                             int sortOrder, String createdAt, List<ChapterNode> chapters) {

        public static VolumeNode from(Volume v, List<ChapterNode> chapters) {
            return new VolumeNode(v.id().toString(), v.projectId().toString(), v.title(), v.conflict(),
                    v.sortOrder(), v.createdAt().toString(), chapters);
        }
    }

    public record ChapterNode(String id, String volumeId, String title, String outline,
                              int wordCount, int sortOrder, String status, String createdAt, String updatedAt) {

        public static ChapterNode from(Chapter c) {
            return new ChapterNode(c.id().toString(), c.volumeId().toString(), c.title(), c.outline(),
                    c.wordCount(), c.sortOrder(), c.status().value(), c.createdAt().toString(), c.updatedAt().toString());
        }
    }
}