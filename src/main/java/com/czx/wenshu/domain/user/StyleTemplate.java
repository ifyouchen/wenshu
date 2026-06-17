package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StyleTemplate {

    private final UUID id;
    private final UUID userId;
    private String name;
    private String templateType;
    private String genres;
    private String prompt;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private StyleTemplate(UUID id, UUID userId, String name, String templateType, String genres,
                          String prompt, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.name = name;
        this.templateType = templateType;
        this.genres = genres != null ? genres : "[]";
        this.prompt = prompt;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static StyleTemplate create(UUID userId, String name, String templateType, String genres,
                                       String prompt, Clock clock) {
        Instant now = Instant.now(clock);
        return new StyleTemplate(UUID.randomUUID(), userId, name, templateType, genres, prompt, false, now, now);
    }

    public static StyleTemplate rehydrate(UUID id, UUID userId, String name, String templateType,
                                          String genres, String prompt, boolean active,
                                          Instant createdAt, Instant updatedAt) {
        return new StyleTemplate(id, userId, name, templateType, genres, prompt, active, createdAt, updatedAt);
    }

    public void update(String name, String templateType, String genres, String prompt, Clock clock) {
        if (name != null) this.name = name;
        if (templateType != null) this.templateType = templateType;
        if (genres != null) this.genres = genres;
        if (prompt != null) this.prompt = prompt;
        this.updatedAt = Instant.now(clock);
    }

    public void activate(Clock clock) {
        this.active = true;
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String name() { return name; }
    public String templateType() { return templateType; }
    public String genres() { return genres; }
    public String prompt() { return prompt; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
