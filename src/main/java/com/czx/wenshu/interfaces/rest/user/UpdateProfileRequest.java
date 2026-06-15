package com.czx.wenshu.interfaces.rest.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "昵称不能超过 100 个字符")
        String nickname,
        String avatarUrl,
        String identityType
) {
}