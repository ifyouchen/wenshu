package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 用户订阅关系领域对象（P9-01/P9-02）。
 *
 * <p>记录某用户当前激活的套餐及有效期。
 * 免费用户在首次查询时通过 {@link #createFree} 创建默认记录。</p>
 *
 * <p>订阅状态值：</p>
 * <ul>
 *   <li>{@code active} — 有效中</li>
 *   <li>{@code cancelled} — 已取消自动续费，但到期前仍有效</li>
 *   <li>{@code expired} — 已到期</li>
 * </ul>
 */
public class UserSubscription {

    /** 免费套餐标识键。 */
    public static final String FREE_PLAN_KEY = "free";

    /** 订阅记录主键。 */
    private final UUID id;

    /** 所属用户 ID。 */
    private final UUID userId;

    /** 当前套餐标识键。 */
    private final String planKey;

    /** 订阅开始时间。 */
    private final Instant startedAt;

    /** 订阅到期时间（免费版为 null，表示永不过期）。 */
    private final Instant expiresAt;

    /** 订阅状态。 */
    private String status;

    /** 全参数私有构造器。 */
    private UserSubscription(UUID id, UUID userId, String planKey,
                             Instant startedAt, Instant expiresAt, String status) {
        this.id = id;
        this.userId = userId;
        this.planKey = planKey;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    /**
     * 为新用户创建默认免费订阅记录。
     *
     * @param userId 用户 ID
     * @param clock  时钟（用于设置 startedAt）
     * @return 新的免费订阅记录
     */
    public static UserSubscription createFree(UUID userId, Clock clock) {
        return new UserSubscription(
                UUID.randomUUID(), userId, FREE_PLAN_KEY,
                clock.instant(), null, "active");
    }

    /**
     * 从持久层还原订阅对象。
     *
     * @param id        主键
     * @param userId    用户 ID
     * @param planKey   套餐 key
     * @param startedAt 开始时间
     * @param expiresAt 到期时间（可为 null）
     * @param status    状态
     * @return 还原后的订阅对象
     */
    public static UserSubscription rehydrate(UUID id, UUID userId, String planKey,
                                             Instant startedAt, Instant expiresAt, String status) {
        return new UserSubscription(id, userId, planKey, startedAt, expiresAt, status);
    }

    /**
     * 判断订阅是否当前有效（active 且未过期）。
     *
     * @param clock 时钟
     * @return true 表示有效
     */
    public boolean isEffective(Clock clock) {
        if (!"active".equals(status) && !"cancelled".equals(status)) return false;
        if (expiresAt == null) return true;  // 无到期时间（免费版）视为永久有效
        return clock.instant().isBefore(expiresAt);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 订阅记录主键 */
    public UUID id() { return id; }

    /** @return 所属用户 ID */
    public UUID userId() { return userId; }

    /** @return 当前套餐 key */
    public String planKey() { return planKey; }

    /** @return 订阅开始时间 */
    public Instant startedAt() { return startedAt; }

    /** @return 订阅到期时间（可为 null） */
    public Instant expiresAt() { return expiresAt; }

    /** @return 订阅状态 */
    public String status() { return status; }
}
