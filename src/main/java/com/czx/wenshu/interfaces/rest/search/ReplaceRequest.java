package com.czx.wenshu.interfaces.rest.search;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record ReplaceRequest(
        @NotBlank String keyword,
        String replacement,
        boolean caseSensitive,
        boolean wholeWord,
        List<UUID> chapterIds,
        boolean syncCharacterName) {
}
