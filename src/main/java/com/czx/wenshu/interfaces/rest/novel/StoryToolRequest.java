package com.czx.wenshu.interfaces.rest.novel;

import java.util.UUID;

public record StoryToolRequest(
        UUID projectId,
        UUID chapterId,
        String input,
        String instruction,
        Integer targetWords
) {
}
