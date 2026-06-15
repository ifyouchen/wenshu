package com.czx.wenshu.application.project;

import jakarta.validation.constraints.NotBlank;

public record CreateChapterCommand(
        @NotBlank(message = "章节标题不能为空") String title,
        String outline,
        int sortOrder
) {
}