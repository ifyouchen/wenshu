package com.czx.wenshu.application.novel;

public record StoryToolInfo(
        String id,
        String name,
        String description,
        String modelLane
) {

    public static StoryToolInfo from(StoryToolKind kind) {
        return new StoryToolInfo(
                kind.id(),
                kind.displayName(),
                kind.description(),
                kind.modelLane().name().toLowerCase()
        );
    }
}
