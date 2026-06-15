package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Project {

    private final UUID id;
    private final UUID userId;
    private String title;
    private String genre;
    private String synopsis;
    private String worldview;
    private int totalWords;
    private int dailyCharGoal;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Project(UUID id, UUID userId, String title, String genre, String synopsis, String worldview,
                    int totalWords, int dailyCharGoal, ProjectStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        setTitle(title);
        this.genre = genre;
        this.synopsis = synopsis;
        this.worldview = worldview;
        this.totalWords = totalWords;
        this.dailyCharGoal = dailyCharGoal;
        this.status = status == null ? ProjectStatus.DRAFT : status;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Project create(UUID userId, String title, String genre, String synopsis, String worldview, Clock clock) {
        Instant now = Instant.now(clock);
        return new Project(UUID.randomUUID(), userId, title, genre, synopsis, worldview, 0, 0, ProjectStatus.DRAFT, now, now);
    }

    public static Project rehydrate(UUID id, UUID userId, String title, String genre, String synopsis, String worldview,
                                     int totalWords, int dailyCharGoal, ProjectStatus status, Instant createdAt, Instant updatedAt) {
        return new Project(id, userId, title, genre, synopsis, worldview, totalWords, dailyCharGoal, status, createdAt, updatedAt);
    }

    public void update(String title, String genre, String synopsis, String worldview, Clock clock) {
        if (title != null) {
            setTitle(title);
        }
        if (genre != null) {
            this.genre = genre.trim().isEmpty() ? null : genre.trim();
        }
        this.synopsis = synopsis;
        this.worldview = worldview;
        this.updatedAt = Instant.now(clock);
    }

    public void updateDailyCharGoal(int dailyCharGoal, Clock clock) {
        this.dailyCharGoal = dailyCharGoal;
        this.updatedAt = Instant.now(clock);
    }

    public void markDeleted(Clock clock) {
        this.status = ProjectStatus.DELETED;
        this.updatedAt = Instant.now(clock);
    }

    private void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("作品标题不能为空");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("作品标题不能超过 200 个字符");
        }
        this.title = trimmed;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String title() {
        return title;
    }

    public String genre() {
        return genre;
    }

    public String synopsis() {
        return synopsis;
    }

    public String worldview() {
        return worldview;
    }

    public int totalWords() {
        return totalWords;
    }

    public int dailyCharGoal() {
        return dailyCharGoal;
    }

    public ProjectStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}