package com.czx.wenshu.domain.imports;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 文件导入解析会话，TTL 24 小时，过期后不允许应用入库。 */
public class ImportParseSession {

    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final UUID id;
    private final UUID projectId;
    private final UUID userId;
    private List<ParsedChapterItem> chapters;
    private final Instant expiresAt;
    private final Instant createdAt;

    private ImportParseSession(UUID id, UUID projectId, UUID userId,
                                List<ParsedChapterItem> chapters,
                                Instant expiresAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.projectId = Objects.requireNonNull(projectId);
        this.userId = Objects.requireNonNull(userId);
        this.chapters = Objects.requireNonNull(chapters);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static ImportParseSession create(UUID projectId, UUID userId,
                                             List<ParsedChapterItem> chapters, Clock clock) {
        Instant now = Instant.now(clock);
        return new ImportParseSession(UUID.randomUUID(), projectId, userId,
                chapters, now.plus(SESSION_TTL), now);
    }

    public static ImportParseSession rehydrate(UUID id, UUID projectId, UUID userId,
                                                List<ParsedChapterItem> chapters,
                                                Instant expiresAt, Instant createdAt) {
        return new ImportParseSession(id, projectId, userId, chapters, expiresAt, createdAt);
    }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void updateChapters(List<ParsedChapterItem> newChapters) {
        this.chapters = List.copyOf(newChapters);
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public UUID userId() { return userId; }
    public List<ParsedChapterItem> chapters() { return chapters; }
    public Instant expiresAt() { return expiresAt; }
    public Instant createdAt() { return createdAt; }
}
