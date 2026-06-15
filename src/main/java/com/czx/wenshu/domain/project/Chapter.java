package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Chapter {

    private final UUID id;
    private final UUID volumeId;
    private final UUID projectId;
    private String title;
    private String outline;
    private String content;
    private int wordCount;
    private int sortOrder;
    private ChapterStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Chapter(UUID id, UUID volumeId, UUID projectId, String title, String outline, String content,
                    int wordCount, int sortOrder, ChapterStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.volumeId = Objects.requireNonNull(volumeId, "volumeId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "projectId must not be null");
        this.title = title;
        this.outline = outline;
        this.content = content != null ? content : "";
        this.wordCount = wordCount;
        this.sortOrder = sortOrder;
        this.status = status == null ? ChapterStatus.PENDING : status;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static Chapter create(UUID volumeId, UUID projectId, String title, String outline, int sortOrder, Clock clock) {
        Instant now = Instant.now(clock);
        return new Chapter(UUID.randomUUID(), volumeId, projectId, title, outline, "", 0, sortOrder, ChapterStatus.PENDING, now, now);
    }

    public static Chapter rehydrate(UUID id, UUID volumeId, UUID projectId, String title, String outline, String content,
                                    int wordCount, int sortOrder, ChapterStatus status, Instant createdAt, Instant updatedAt) {
        return new Chapter(id, volumeId, projectId, title, outline, content, wordCount, sortOrder, status, createdAt, updatedAt);
    }

    public void saveContent(String title, String content, String outline, ChapterStatus status, Clock clock) {
        if (title != null) {
            this.title = title;
        }
        if (outline != null) {
            this.outline = outline;
        }
        if (content != null) {
            this.wordCount = countWords(content);
            this.content = content;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = Instant.now(clock);
    }

    public int wordCountDelta(String newContent) {
        if (newContent == null) {
            return 0;
        }
        return countWords(newContent) - this.wordCount;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.length();
    }

    public UUID id() {
        return id;
    }

    public UUID volumeId() {
        return volumeId;
    }

    public UUID projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public String outline() {
        return outline;
    }

    public String content() {
        return content;
    }

    public int wordCount() {
        return wordCount;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public ChapterStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}