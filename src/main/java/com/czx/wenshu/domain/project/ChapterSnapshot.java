package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ChapterSnapshot {

    private final UUID id;
    private final UUID chapterId;
    private String content;
    private int wordCount;
    private String snapshotType;
    private String label;
    private final Instant createdAt;

    private ChapterSnapshot(UUID id, UUID chapterId, String content, int wordCount,
                            String snapshotType, String label, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.chapterId = Objects.requireNonNull(chapterId, "chapterId must not be null");
        this.content = content != null ? content : "";
        this.wordCount = wordCount;
        this.snapshotType = snapshotType;
        this.label = label;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static ChapterSnapshot create(UUID chapterId, String content, int wordCount,
                                          String snapshotType, String label, Clock clock) {
        return new ChapterSnapshot(UUID.randomUUID(), chapterId, content != null ? content : "",
                wordCount, snapshotType, label, Instant.now(clock));
    }

    public static ChapterSnapshot rehydrate(UUID id, UUID chapterId, String content, int wordCount,
                                             String snapshotType, String label, Instant createdAt) {
        return new ChapterSnapshot(id, chapterId, content, wordCount, snapshotType, label, createdAt);
    }

    public UUID id() { return id; }
    public UUID chapterId() { return chapterId; }
    public String content() { return content; }
    public int wordCount() { return wordCount; }
    public String snapshotType() { return snapshotType; }
    public String label() { return label; }
    public Instant createdAt() { return createdAt; }
}