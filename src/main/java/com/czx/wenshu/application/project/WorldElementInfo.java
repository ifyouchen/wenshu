package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.WorldElement;

public record WorldElementInfo(String id, String projectId, String type, String name,
                                String description, boolean locked, String createdAt) {

    public static WorldElementInfo from(WorldElement e) {
        return new WorldElementInfo(e.id().toString(), e.projectId().toString(), e.type(), e.name(),
                e.description(), e.locked(), e.createdAt().toString());
    }
}