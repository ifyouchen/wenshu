package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class WorldElement {

    private final UUID id;
    private final UUID projectId;
    private String type;
    private String name;
    private String description;
    private String aliases;
    private boolean locked;
    private final Instant createdAt;

    private WorldElement(UUID id, UUID projectId, String type, String name, String description,
                         String aliases, boolean locked, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.type = type;
        this.name = name;
        this.description = description;
        this.aliases = aliases != null ? aliases : "[]";
        this.locked = locked;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static WorldElement create(UUID projectId, String type, String name, String description,
                                       String aliases, Clock clock) {
        return new WorldElement(UUID.randomUUID(), projectId, type, name, description,
                aliases, false, Instant.now(clock));
    }

    public static WorldElement rehydrate(UUID id, UUID projectId, String type, String name,
                                          String description, String aliases, boolean locked, Instant createdAt) {
        return new WorldElement(id, projectId, type, name, description, aliases, locked, createdAt);
    }

    public void update(String type, String name, String description, String aliases) {
        if (type != null) this.type = type;
        if (name != null) this.name = name;
        this.description = description;
        if (aliases != null) this.aliases = aliases;
    }

    public void toggleLock() {
        this.locked = !this.locked;
    }

    /** 仅更新名称，用于角色名同步策略（P3-05）。 */
    public void syncName(String newName) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName;
        }
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public String type() { return type; }
    public String name() { return name; }
    public String description() { return description; }
    public String aliases() { return aliases; }
    public boolean locked() { return locked; }
    public Instant createdAt() { return createdAt; }
}