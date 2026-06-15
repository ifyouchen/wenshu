package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Volume {

    private final UUID id;
    private final UUID projectId;
    private String title;
    private String conflict;
    private int sortOrder;
    private final Instant createdAt;

    private Volume(UUID id, UUID projectId, String title, String conflict, int sortOrder, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.title = title;
        this.conflict = conflict;
        this.sortOrder = sortOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Volume create(UUID projectId, String title, String conflict, int sortOrder, Clock clock) {
        return new Volume(UUID.randomUUID(), projectId, title, conflict, sortOrder, Instant.now(clock));
    }

    public static Volume rehydrate(UUID id, UUID projectId, String title, String conflict, int sortOrder, Instant createdAt) {
        return new Volume(id, projectId, title, conflict, sortOrder, createdAt);
    }

    public void update(String title, String conflict) {
        this.title = title;
        this.conflict = conflict;
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public String conflict() {
        return conflict;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public Instant createdAt() {
        return createdAt;
    }
}