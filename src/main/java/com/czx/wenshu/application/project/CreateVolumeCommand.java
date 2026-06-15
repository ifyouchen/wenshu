package com.czx.wenshu.application.project;

import jakarta.validation.constraints.NotBlank;

public record CreateVolumeCommand(
        @NotBlank(message = "卷标题不能为空") String title,
        String conflict,
        int sortOrder
) {
}