package com.czx.wenshu.domain.team;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 团队领域对象（P9-07）。
 *
 * <p>团队是企业版用户的协作单位，拥有：</p>
 * <ul>
 *   <li>创建者（owner，默认拥有 admin 权限）</li>
 *   <li>共享配额池（所有成员共用月度 AI 字符额度和改编次数）</li>
 *   <li>成员管理（admin 可邀请/移除成员）</li>
 * </ul>
 */
public class Team {

    /** 团队主键。 */
    private final UUID id;

    /** 团队创建者/所有者 ID。 */
    private final UUID ownerId;

    /** 团队名称。 */
    private String name;

    /** 团队套餐 key（默认 enterprise）。 */
    private final String planKey;

    /** 团队共享月度字符额度。 */
    private final long monthlyCharLimit;

    /** 团队共享月度改编/审查次数额度。 */
    private final int monthlyAdaptationLimit;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 私有全参数构造器。 */
    private Team(UUID id, UUID ownerId, String name, String planKey,
                 long monthlyCharLimit, int monthlyAdaptationLimit,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.planKey = planKey;
        this.monthlyCharLimit = monthlyCharLimit;
        this.monthlyAdaptationLimit = monthlyAdaptationLimit;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新团队。
     *
     * @param ownerId 创建者 ID
     * @param name    团队名称
     * @param clock   时钟
     * @return 新团队对象
     */
    public static Team create(UUID ownerId, String name, Clock clock) {
        Instant now = clock.instant();
        return new Team(UUID.randomUUID(), ownerId, name, "enterprise",
                10_000_000L, 200, now, now);
    }

    /**
     * 从持久层还原团队对象。
     */
    public static Team rehydrate(UUID id, UUID ownerId, String name, String planKey,
                                 long monthlyCharLimit, int monthlyAdaptationLimit,
                                 Instant createdAt, Instant updatedAt) {
        return new Team(id, ownerId, name, planKey, monthlyCharLimit,
                monthlyAdaptationLimit, createdAt, updatedAt);
    }

    /**
     * 更新团队名称。
     *
     * @param newName 新名称
     * @param clock   时钟
     */
    public void updateName(String newName, Clock clock) {
        this.name = newName;
        this.updatedAt = clock.instant();
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 团队主键 */
    public UUID id() { return id; }

    /** @return 所有者 ID */
    public UUID ownerId() { return ownerId; }

    /** @return 团队名称 */
    public String name() { return name; }

    /** @return 套餐 key */
    public String planKey() { return planKey; }

    /** @return 月度字符额度 */
    public long monthlyCharLimit() { return monthlyCharLimit; }

    /** @return 月度改编次数额度 */
    public int monthlyAdaptationLimit() { return monthlyAdaptationLimit; }

    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }

    /** @return 更新时间 */
    public Instant updatedAt() { return updatedAt; }
}
