package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.Project;

public record ProjectInfo(
        String id, String userId, String title, String genre, String synopsis, String worldview,
        int totalWords, int dailyCharGoal, String status, String createdAt, String updatedAt) {

    public static ProjectInfo from(Project p) {
        return new ProjectInfo(p.id().toString(), p.userId().toString(), p.title(), p.genre(), p.synopsis(),
                p.worldview(), p.totalWords(), p.dailyCharGoal(), p.status().value(),
                p.createdAt().toString(), p.updatedAt().toString());
    }
}