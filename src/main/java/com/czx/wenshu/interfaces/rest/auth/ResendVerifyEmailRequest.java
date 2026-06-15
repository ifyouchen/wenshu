package com.czx.wenshu.interfaces.rest.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerifyEmailRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}
