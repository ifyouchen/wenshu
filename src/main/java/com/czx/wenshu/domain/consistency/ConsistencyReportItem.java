package com.czx.wenshu.domain.consistency;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 一致性审查报告条目（P6-06/P6-07）。
 * 记录 LLM 发现的具体问题，支持用户标记处理状态。
 */
public class ConsistencyReportItem {

    /** 条目唯一 ID。 */
    private final UUID id;
    /** 关联的报告 ID（ai_operation_logs.id）。 */
    private final UUID reportId;
    /** 所属作品 ID。 */
    private final UUID projectId;
    /** 问题类型：character/timeline/location/plot。 */
    private final String type;
    /** 涉及角色名称（可为 null）。 */
    private final String character;
    /** 涉及章节提示（可为 null）。 */
    private final String chapterHint;
    /** 问题描述。 */
    private final String description;
    /** 修改建议（可为 null）。 */
    private final String suggestion;
    /**
     * 处理状态：open（待处理）/ handled（已处理）/ ignored（已忽略）。
     */
    private String status;
    /** 创建时间。 */
    private final Instant createdAt;
    /** 最后更新时间。 */
    private Instant updatedAt;

    private ConsistencyReportItem(UUID id, UUID reportId, UUID projectId, String type,
                                   String character, String chapterHint, String description,
                                   String suggestion, String status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.reportId = Objects.requireNonNull(reportId, "reportId 不能为空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不能为空");
        this.type = type;
        this.character = character;
        this.chapterHint = chapterHint;
        this.description = Objects.requireNonNull(description, "description 不能为空");
        this.suggestion = suggestion;
        this.status = status != null ? status : "open";
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /**
     * 创建新审查条目。
     */
    public static ConsistencyReportItem create(UUID reportId, UUID projectId, String type,
                                                String character, String chapterHint,
                                                String description, String suggestion, Clock clock) {
        Instant now = Instant.now(clock);
        return new ConsistencyReportItem(UUID.randomUUID(), reportId, projectId, type,
                character, chapterHint, description, suggestion, "open", now, now);
    }

    /**
     * 从持久化记录重建。
     */
    public static ConsistencyReportItem rehydrate(UUID id, UUID reportId, UUID projectId,
                                                    String type, String character, String chapterHint,
                                                    String description, String suggestion, String status,
                                                    Instant createdAt, Instant updatedAt) {
        return new ConsistencyReportItem(id, reportId, projectId, type, character, chapterHint,
                description, suggestion, status, createdAt, updatedAt);
    }

    /**
     * 更新处理状态（P6-07）。
     * 允许值：open / handled / ignored。
     *
     * @param newStatus 新状态
     * @param clock     时钟
     */
    public void updateStatus(String newStatus, Clock clock) {
        if (!"open".equals(newStatus) && !"handled".equals(newStatus) && !"ignored".equals(newStatus)) {
            throw new IllegalArgumentException("无效状态：" + newStatus + "，允许值：open/handled/ignored");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID reportId() { return reportId; }
    public UUID projectId() { return projectId; }
    public String type() { return type; }
    public String character() { return character; }
    public String chapterHint() { return chapterHint; }
    public String description() { return description; }
    public String suggestion() { return suggestion; }
    public String status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
