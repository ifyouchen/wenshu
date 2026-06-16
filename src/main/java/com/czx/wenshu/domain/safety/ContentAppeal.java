package com.czx.wenshu.domain.safety;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 内容安全申诉领域对象（P9-05）。
 *
 * <p>用户对 AI 输出被内容安全过滤器误判后，可提交申诉说明理由。</p>
 *
 * <p>申诉状态流转：</p>
 * <ul>
 *   <li>{@code pending} — 待审核（初始状态）</li>
 *   <li>{@code approved} — 申诉通过（内容确认无误，可重试）</li>
 *   <li>{@code rejected} — 申诉驳回（内容确实违规）</li>
 * </ul>
 */
public class ContentAppeal {

    /** 申诉记录主键。 */
    private final UUID id;

    /** 提交申诉的用户 ID。 */
    private final UUID userId;

    /** 被过滤的 AI 输出内容片段。 */
    private final String content;

    /** 用户申诉理由。 */
    private final String reason;

    /** 当前审核状态（pending / approved / rejected）。 */
    private String status;

    /** 审核人备注（可选）。 */
    private String reviewerNote;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 最后更新时间。 */
    private Instant updatedAt;

    /** 全参数私有构造器。 */
    private ContentAppeal(UUID id, UUID userId, String content, String reason,
                          String status, String reviewerNote,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.reason = reason;
        this.status = status;
        this.reviewerNote = reviewerNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新申诉记录（状态为 pending）。
     *
     * @param userId  申诉用户 ID
     * @param content 被过滤的内容片段
     * @param reason  申诉理由
     * @param clock   时钟
     * @return 新申诉对象
     */
    public static ContentAppeal create(UUID userId, String content, String reason, Clock clock) {
        Instant now = clock.instant();
        return new ContentAppeal(UUID.randomUUID(), userId, content, reason,
                "pending", null, now, now);
    }

    /**
     * 从持久层还原申诉对象。
     *
     * @param id           主键
     * @param userId       用户 ID
     * @param content      被过滤内容
     * @param reason       申诉理由
     * @param status       审核状态
     * @param reviewerNote 审核备注
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @return 还原的申诉对象
     */
    public static ContentAppeal rehydrate(UUID id, UUID userId, String content, String reason,
                                          String status, String reviewerNote,
                                          Instant createdAt, Instant updatedAt) {
        return new ContentAppeal(id, userId, content, reason, status, reviewerNote,
                createdAt, updatedAt);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 申诉记录主键 */
    public UUID id() { return id; }

    /** @return 申诉用户 ID */
    public UUID userId() { return userId; }

    /** @return 被过滤的内容片段 */
    public String content() { return content; }

    /** @return 申诉理由 */
    public String reason() { return reason; }

    /** @return 当前审核状态 */
    public String status() { return status; }

    /** @return 审核人备注（可为 null） */
    public String reviewerNote() { return reviewerNote; }

    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }

    /** @return 最后更新时间 */
    public Instant updatedAt() { return updatedAt; }
}
