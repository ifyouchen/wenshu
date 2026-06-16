package com.czx.wenshu.interfaces.rest.novel;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BranchRequest(@NotNull UUID chapterId, int branchCount) {
}
