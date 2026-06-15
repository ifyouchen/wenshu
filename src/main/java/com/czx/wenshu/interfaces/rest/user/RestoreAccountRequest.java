package com.czx.wenshu.interfaces.rest.user;

import jakarta.validation.constraints.NotBlank;

public record RestoreAccountRequest(
        @NotBlank(message = "恢复令牌不能为空")
        String restoreToken
) {
}