package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 用户文风档案（P5-10）：存储写作样本和异步分析生成的风格标签。 */
public class UserStyleProfile {

    private final UUID id;
    private final UUID userId;
    private String sampleText;
    private String styleTags;
    private UUID analysisTaskId;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserStyleProfile(UUID id, UUID userId, String sampleText, String styleTags,
                              UUID analysisTaskId, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.sampleText = sampleText;
        this.styleTags = styleTags != null ? styleTags : "[]";
        this.analysisTaskId = analysisTaskId;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static UserStyleProfile create(UUID userId, String sampleText, Clock clock) {
        Instant now = Instant.now(clock);
        return new UserStyleProfile(UUID.randomUUID(), userId, sampleText, "[]", null, now, now);
    }

    public static UserStyleProfile rehydrate(UUID id, UUID userId, String sampleText,
                                              String styleTags, UUID analysisTaskId,
                                              Instant createdAt, Instant updatedAt) {
        return new UserStyleProfile(id, userId, sampleText, styleTags, analysisTaskId, createdAt, updatedAt);
    }

    public void updateSample(String sampleText, UUID taskId, Clock clock) {
        this.sampleText = sampleText;
        this.analysisTaskId = taskId;
        this.styleTags = "[]";
        this.updatedAt = Instant.now(clock);
    }

    public void updateTags(String styleTags, Clock clock) {
        this.styleTags = styleTags != null ? styleTags : "[]";
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String sampleText() { return sampleText; }
    public String styleTags() { return styleTags; }
    public UUID analysisTaskId() { return analysisTaskId; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
