package com.czx.wenshu.interfaces.rest.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 128, message = "密码长度须在 8 到 128 个字符之间")
        String newPassword
) {
}