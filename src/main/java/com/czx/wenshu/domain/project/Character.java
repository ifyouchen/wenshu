package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Character {

    private final UUID id;
    private final UUID projectId;
    private String name;
    private String role;
    private String appearance;
    private String personality;
    private String abilities;
    private String speechStyle;
    private String status;
    private boolean locked;
    private UUID firstChapterId;
    private UUID lastActiveChapterId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Character(UUID id, UUID projectId, String name, String role, String appearance, String personality,
                      String abilities, String speechStyle, String status, boolean locked,
                      UUID firstChapterId, UUID lastActiveChapterId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.name = name;
        this.role = role;
        this.appearance = appearance;
        this.personality = personality;
        this.abilities = abilities;
        this.speechStyle = speechStyle;
        this.status = status;
        this.locked = locked;
        this.firstChapterId = firstChapterId;
        this.lastActiveChapterId = lastActiveChapterId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Character create(UUID projectId, String name, String role, Clock clock) {
        Instant now = Instant.now(clock);
        return new Character(UUID.randomUUID(), projectId, name, role, null, null, "[]", null, "{}", false, null, null, now, now);
    }

    public static Character rehydrate(UUID id, UUID projectId, String name, String role, String appearance,
                                      String personality, String abilities, String speechStyle, String status,
                                      boolean locked, UUID firstChapterId, UUID lastActiveChapterId,
                                      Instant createdAt, Instant updatedAt) {
        return new Character(id, projectId, name, role, appearance, personality, abilities, speechStyle,
                status, locked, firstChapterId, lastActiveChapterId, createdAt, updatedAt);
    }

    public void update(String name, String role, String appearance, String personality,
                       String abilities, String speechStyle, String status, Clock clock) {
        if (name != null) this.name = name;
        if (role != null) this.role = role;
        this.appearance = appearance;
        this.personality = personality;
        if (abilities != null) this.abilities = abilities;
        if (speechStyle != null) this.speechStyle = speechStyle;
        if (status != null) this.status = status;
        this.updatedAt = Instant.now(clock);
    }

    public void toggleLock(Clock clock) {
        this.locked = !this.locked;
        this.updatedAt = Instant.now(clock);
    }

    /**
     * P6-02：更新角色在章节中的锚点。
     * 若 firstChapterId 为空则同时记录为首次出现章节。
     */
    public void updateAnchor(UUID chapterId, Clock clock) {
        this.lastActiveChapterId = chapterId;
        if (this.firstChapterId == null) {
            this.firstChapterId = chapterId;
        }
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public String name() { return name; }
    public String role() { return role; }
    public String appearance() { return appearance; }
    public String personality() { return personality; }
    public String abilities() { return abilities; }
    public String speechStyle() { return speechStyle; }
    public String status() { return status; }
    public boolean locked() { return locked; }
    public UUID firstChapterId() { return firstChapterId; }
    public UUID lastActiveChapterId() { return lastActiveChapterId; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}