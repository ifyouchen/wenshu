package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Character;

public record CharacterInfo(String id, String projectId, String name, String role, String appearance,
                             String personality, String abilities, String speechStyle, String status,
                             boolean locked, String firstChapterId, String lastActiveChapterId,
                             String createdAt, String updatedAt) {

    public static CharacterInfo from(Character c) {
        return new CharacterInfo(c.id().toString(), c.projectId().toString(), c.name(), c.role(),
                c.appearance(), c.personality(), c.abilities(), c.speechStyle(), c.status(),
                c.locked(),
                c.firstChapterId() != null ? c.firstChapterId().toString() : null,
                c.lastActiveChapterId() != null ? c.lastActiveChapterId().toString() : null,
                c.createdAt().toString(), c.updatedAt().toString());
    }
}