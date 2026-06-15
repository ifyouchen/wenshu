package com.czx.wenshu.interfaces.rest.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateIdentityTypeRequest(
        @NotBlank(message = "身份类型不能为空")
        String identityType
) {
}