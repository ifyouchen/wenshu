package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** AI 生成的章节摘要（P6-01），供动态上下文组装时使用。 */
public class ChapterSummary {

    private final UUID id;
    private final UUID chapterId;
    private final UUID projectId;
    private String summary;
    private final Instant createdAt;

    private ChapterSummary(UUID id, UUID chapterId, UUID projectId, String summary, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.chapterId = Objects.requireNonNull(chapterId);
        this.projectId = Objects.requireNonNull(projectId);
        this.summary = Objects.requireNonNull(summary);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static ChapterSummary create(UUID chapterId, UUID projectId, String summary, Clock clock) {
        return new ChapterSummary(UUID.randomUUID(), chapterId, projectId, summary, Instant.now(clock));
    }

    public static ChapterSummary rehydrate(UUID id, UUID chapterId, UUID projectId,
                                            String summary, Instant createdAt) {
        return new ChapterSummary(id, chapterId, projectId, summary, createdAt);
    }

    public UUID id() { return id; }
    public UUID chapterId() { return chapterId; }
    public UUID projectId() { return projectId; }
    public String summary() { return summary; }
    public Instant createdAt() { return createdAt; }
}
