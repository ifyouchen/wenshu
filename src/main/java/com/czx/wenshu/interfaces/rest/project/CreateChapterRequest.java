package com.czx.wenshu.interfaces.rest.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChapterRequest(
        @NotBlank(message = "章节标题不能为空") @Size(max = 200, message = "章节标题不能超过 200 个字符")
        String title,
        String outline,
        int sortOrder
) {
}