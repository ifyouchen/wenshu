package com.czx.wenshu.interfaces.rest.novel;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SkeletonRequest(
        @NotNull UUID projectId,
        String genre,
        String synopsis,
        String worldview,
        Integer targetWords) {
}
