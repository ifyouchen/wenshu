package com.czx.wenshu.application.novel;

/** 骨架应用入库结果（P5-05）。 */
public record SkeletonApplyResult(
        String projectId,
        int createdVolumes,
        int createdChapters,
        int createdCharacters,
        String title,
        String theme) {
}
