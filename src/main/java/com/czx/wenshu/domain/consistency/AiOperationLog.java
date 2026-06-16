package com.czx.wenshu.domain.consistency;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * AI 操作日志（P6-06）。
 * 同时作为一致性审查报告的容器，report_id 即为本对象的 id。
 */
public class AiOperationLog {

    /** 日志/报告唯一 ID。 */
    private final UUID id;
    /** 执行操作的用户 ID。 */
    private final UUID userId;
    /** 所属作品 ID（可为 null）。 */
    private final UUID projectId;
    /** 操作类型，例如 "consistency_check"。 */
    private final String operation;
    /** 使用的 LLM 模型名称。 */
    private final String model;
    /** 创建时间。 */
    private final Instant createdAt;

    private AiOperationLog(UUID id, UUID userId, UUID projectId, String operation,
                            String model, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.userId = Objects.requireNonNull(userId, "userId 不能为空");
        this.projectId = projectId;
        this.operation = Objects.requireNonNull(operation, "operation 不能为空");
        this.model = model;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
    }

    /**
     * 创建新的操作日志条目。
     *
     * @param userId    用户 ID
     * @param projectId 作品 ID（可为 null）
     * @param operation 操作类型
     * @param model     使用模型名称
     * @param now       当前时间戳
     */
    public static AiOperationLog create(UUID userId, UUID projectId, String operation,
                                         String model, Instant now) {
        return new AiOperationLog(UUID.randomUUID(), userId, projectId, operation, model, now);
    }

    /**
     * 从持久化记录重建。
     */
    public static AiOperationLog rehydrate(UUID id, UUID userId, UUID projectId,
                                            String operation, String model, Instant createdAt) {
        return new AiOperationLog(id, userId, projectId, operation, model, createdAt);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID projectId() { return projectId; }
    public String operation() { return operation; }
    public String model() { return model; }
    public Instant createdAt() { return createdAt; }
}
