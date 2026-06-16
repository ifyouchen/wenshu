package com.czx.wenshu.interfaces.rest.imports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PasteImportRequest(@NotNull UUID projectId,
                                  @NotNull UUID volumeId,
                                  @NotBlank String text) {
}
