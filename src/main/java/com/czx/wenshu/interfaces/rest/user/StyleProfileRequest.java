package com.czx.wenshu.interfaces.rest.user;

import jakarta.validation.constraints.NotBlank;

public record StyleProfileRequest(@NotBlank String sampleText) {
}
