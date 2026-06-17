package com.czx.wenshu.application.novel;

import java.util.UUID;

public record StoryToolResult(
        String tool,
        UUID projectId,
        UUID chapterId,
        String output
) {
}
