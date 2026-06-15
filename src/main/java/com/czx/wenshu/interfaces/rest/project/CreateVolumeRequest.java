package com.czx.wenshu.interfaces.rest.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVolumeRequest(
        @NotBlank(message = "卷标题不能为空") @Size(max = 200, message = "卷标题不能超过 200 个字符")
        String title,
        String conflict,
        int sortOrder
) {
}