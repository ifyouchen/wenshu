package com.czx.wenshu.domain.task;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** AI 异步任务进度记录。 */
public class AsyncTask {

    private final UUID id;
    private final UUID userId;
    private final UUID projectId;
    private final String taskType;
    private AsyncTaskStatus status;
    private int currentStep;
    private Integer totalSteps;
    private String stepLabel;
    private int progressPct;
    private UUID resultId;
    private String resultJson;
    private String errorMessage;
    private final Instant createdAt;
    private Instant updatedAt;

    private AsyncTask(UUID id, UUID userId, UUID projectId, String taskType,
                      AsyncTaskStatus status, int currentStep, Integer totalSteps,
                      String stepLabel, int progressPct, UUID resultId, String resultJson,
                      String errorMessage, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.projectId = projectId;
        this.taskType = Objects.requireNonNull(taskType);
        this.status = status != null ? status : AsyncTaskStatus.PENDING;
        this.currentStep = currentStep;
        this.totalSteps = totalSteps;
        this.stepLabel = stepLabel;
        this.progressPct = Math.max(0, Math.min(100, progressPct));
        this.resultId = resultId;
        this.resultJson = resultJson;
        this.errorMessage = errorMessage;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static AsyncTask create(UUID userId, UUID projectId, String taskType, Clock clock) {
        Instant now = Instant.now(clock);
        return new AsyncTask(UUID.randomUUID(), userId, projectId, taskType,
                AsyncTaskStatus.PENDING, 0, null, null, 0, null, null, null, now, now);
    }

    public static AsyncTask rehydrate(UUID id, UUID userId, UUID projectId, String taskType,
                                       AsyncTaskStatus status, int currentStep, Integer totalSteps,
                                       String stepLabel, int progressPct, UUID resultId, String resultJson,
                                       String errorMessage, Instant createdAt, Instant updatedAt) {
        return new AsyncTask(id, userId, projectId, taskType, status, currentStep, totalSteps,
                stepLabel, progressPct, resultId, resultJson, errorMessage, createdAt, updatedAt);
    }

    public void markRunning(int total, String label, Clock clock) {
        this.status = AsyncTaskStatus.RUNNING;
        this.totalSteps = total;
        this.stepLabel = label;
        this.progressPct = 0;
        this.updatedAt = Instant.now(clock);
    }

    public void updateProgress(int current, String label, int pct, Clock clock) {
        this.currentStep = current;
        this.stepLabel = label;
        this.progressPct = Math.max(0, Math.min(100, pct));
        this.updatedAt = Instant.now(clock);
    }

    public void complete(UUID resultId, Clock clock) {
        this.status = AsyncTaskStatus.COMPLETED;
        this.resultId = resultId;
        this.progressPct = 100;
        this.updatedAt = Instant.now(clock);
    }

    public void completeWithJson(String json, Clock clock) {
        this.status = AsyncTaskStatus.COMPLETED;
        this.resultJson = json;
        this.progressPct = 100;
        this.updatedAt = Instant.now(clock);
    }

    public void fail(String errorMessage, Clock clock) {
        this.status = AsyncTaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID projectId() { return projectId; }
    public String taskType() { return taskType; }
    public AsyncTaskStatus status() { return status; }
    public int currentStep() { return currentStep; }
    public Integer totalSteps() { return totalSteps; }
    public String stepLabel() { return stepLabel; }
    public int progressPct() { return progressPct; }
    public UUID resultId() { return resultId; }
    public String resultJson() { return resultJson; }
    public String errorMessage() { return errorMessage; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
