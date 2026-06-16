package com.czx.wenshu.domain.script;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 剧本草稿聚合根（P7-01）。
 * 由用户对小说作品发起改编任务后生成，包含场景列表和集数划分。
 */
public class ScriptDraft {

    /** 草稿唯一 ID。 */
    private final UUID id;
    /** 所属作品 ID。 */
    private final UUID projectId;
    /** 所属用户 ID。 */
    private final UUID userId;
    /** 草稿标题。 */
    private String title;
    /** 改编策略：action/dialogue/voiceover 等。 */
    private String strategy;
    /**
     * 草稿状态：processing（改编中）/ ready（可编辑）/ failed（失败）。
     */
    private String status;
    /** 总场景数（改编完成后填写）。 */
    private Integer totalScenes;
    /** 创建时间。 */
    private final Instant createdAt;
    /** 最后更新时间。 */
    private Instant updatedAt;

    private ScriptDraft(UUID id, UUID projectId, UUID userId, String title, String strategy,
                         String status, Integer totalScenes, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不能为空");
        this.userId = Objects.requireNonNull(userId, "userId 不能为空");
        this.title = title;
        this.strategy = strategy;
        this.status = status != null ? status : "processing";
        this.totalScenes = totalScenes;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /**
     * 创建新剧本草稿。
     *
     * @param projectId 所属作品 ID
     * @param userId    创建用户 ID
     * @param title     草稿标题
     * @param strategy  改编策略
     * @param clock     时钟
     */
    public static ScriptDraft create(UUID projectId, UUID userId, String title,
                                      String strategy, Clock clock) {
        Instant now = Instant.now(clock);
        return new ScriptDraft(UUID.randomUUID(), projectId, userId, title,
                strategy, "processing", null, now, now);
    }

    /**
     * 从持久化记录重建。
     */
    public static ScriptDraft rehydrate(UUID id, UUID projectId, UUID userId, String title,
                                         String strategy, String status, Integer totalScenes,
                                         Instant createdAt, Instant updatedAt) {
        return new ScriptDraft(id, projectId, userId, title, strategy, status,
                totalScenes, createdAt, updatedAt);
    }

    /**
     * 将草稿标记为就绪状态，并记录总场景数。
     *
     * @param totalScenes 总场景数
     * @param clock       时钟
     */
    public void markReady(int totalScenes, Clock clock) {
        this.status = "ready";
        this.totalScenes = totalScenes;
        this.updatedAt = Instant.now(clock);
    }

    /**
     * 将草稿标记为失败状态。
     *
     * @param clock 时钟
     */
    public void markFailed(Clock clock) {
        this.status = "failed";
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public UUID userId() { return userId; }
    public String title() { return title; }
    public String strategy() { return strategy; }
    public String status() { return status; }
    public Integer totalScenes() { return totalScenes; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
