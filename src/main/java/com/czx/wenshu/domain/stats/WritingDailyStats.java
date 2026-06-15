package com.czx.wenshu.domain.stats;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class WritingDailyStats {

    private UUID id;
    private final UUID userId;
    private UUID projectId;
    private LocalDate statDate;
    private int manualChars;
    private int aiAcceptedChars;
    private int totalChars;
    private Instant updatedAt;

    private WritingDailyStats(UUID id, UUID userId, UUID projectId, LocalDate statDate,
                              int manualChars, int aiAcceptedChars, int totalChars, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.projectId = projectId;
        this.statDate = Objects.requireNonNull(statDate, "statDate must not be null");
        this.manualChars = manualChars;
        this.aiAcceptedChars = aiAcceptedChars;
        this.totalChars = totalChars;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static WritingDailyStats create(UUID userId, UUID projectId, LocalDate statDate, int delta, Clock clock) {
        int safeDelta = Math.max(delta, 0);
        return new WritingDailyStats(UUID.randomUUID(), userId, projectId, statDate, safeDelta, 0, safeDelta, Instant.now(clock));
    }

    public static WritingDailyStats rehydrate(UUID id, UUID userId, UUID projectId, LocalDate statDate,
                                               int manualChars, int aiAcceptedChars, int totalChars, Instant updatedAt) {
        return new WritingDailyStats(id, userId, projectId, statDate, manualChars, aiAcceptedChars, totalChars, updatedAt);
    }

    public void addManualDelta(int delta, Clock clock) {
        if (delta > 0) {
            this.manualChars += delta;
            this.totalChars += delta;
        }
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID projectId() { return projectId; }
    public LocalDate statDate() { return statDate; }
    public int manualChars() { return manualChars; }
    public int aiAcceptedChars() { return aiAcceptedChars; }
    public int totalChars() { return totalChars; }
    public Instant updatedAt() { return updatedAt; }
}