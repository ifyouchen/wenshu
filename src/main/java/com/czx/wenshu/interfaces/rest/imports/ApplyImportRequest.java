package com.czx.wenshu.interfaces.rest.imports;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplyImportRequest(@NotNull UUID volumeId) {
}
