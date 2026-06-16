package com.czx.wenshu.interfaces.rest.novel;

import jakarta.validation.constraints.NotBlank;

public record PolishRequest(@NotBlank String text, String instruction, String styleDescription) {
}
