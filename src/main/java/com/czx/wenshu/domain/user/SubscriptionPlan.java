package com.czx.wenshu.domain.user;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订阅套餐领域对象（P9-01/P9-02）。
 *
 * <p>代表系统中可售的订阅套餐，由 {@code subscription_plans} 表持久化。
 * 套餐定义由运营方维护，不由用户操作。</p>
 *
 * <p>关键限额字段：</p>
 * <ul>
 *   <li>{@code monthlyCharLimit} — 每月最大 AI 字符消耗量</li>
 *   <li>{@code monthlyAdaptationLimit} — 每月最大改编/一致性审查次数</li>
 * </ul>
 */
public class SubscriptionPlan {

    /** 套餐主键 ID。 */
    private final UUID id;

    /** 套餐标识键（free / pro / enterprise）。 */
    private final String planKey;

    /** 套餐名称（展示用）。 */
    private final String name;

    /** 每月 AI 字符额度。 */
    private final long monthlyCharLimit;

    /** 每月改编/一致性审查次数额度。 */
    private final int monthlyAdaptationLimit;

    /** 每月价格（人民币元，免费版为 0）。 */
    private final BigDecimal pricePerMonth;

    /** 套餐说明文字。 */
    private final String description;

    /** 是否对外可售（下线套餐设为 false）。 */
    private final boolean active;

    /** 全参数私有构造器，通过静态工厂创建实例。 */
    private SubscriptionPlan(UUID id, String planKey, String name,
                             long monthlyCharLimit, int monthlyAdaptationLimit,
                             BigDecimal pricePerMonth, String description, boolean active) {
        this.id = id;
        this.planKey = planKey;
        this.name = name;
        this.monthlyCharLimit = monthlyCharLimit;
        this.monthlyAdaptationLimit = monthlyAdaptationLimit;
        this.pricePerMonth = pricePerMonth;
        this.description = description;
        this.active = active;
    }

    /**
     * 从持久层还原套餐对象。
     *
     * @param id                     套餐主键
     * @param planKey                套餐标识键
     * @param name                   套餐名称
     * @param monthlyCharLimit       每月字符额度
     * @param monthlyAdaptationLimit 每月改编次数额度
     * @param pricePerMonth          月价格
     * @param description            描述文字
     * @param active                 是否有效
     * @return 还原后的套餐对象
     */
    public static SubscriptionPlan rehydrate(UUID id, String planKey, String name,
                                             long monthlyCharLimit, int monthlyAdaptationLimit,
                                             BigDecimal pricePerMonth, String description,
                                             boolean active) {
        return new SubscriptionPlan(id, planKey, name, monthlyCharLimit, monthlyAdaptationLimit,
                pricePerMonth, description, active);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 套餐主键 ID */
    public UUID id() { return id; }

    /** @return 套餐标识键 */
    public String planKey() { return planKey; }

    /** @return 套餐名称 */
    public String name() { return name; }

    /** @return 每月 AI 字符额度 */
    public long monthlyCharLimit() { return monthlyCharLimit; }

    /** @return 每月改编/审查次数额度 */
    public int monthlyAdaptationLimit() { return monthlyAdaptationLimit; }

    /** @return 每月价格（元） */
    public BigDecimal pricePerMonth() { return pricePerMonth; }

    /** @return 套餐描述 */
    public String description() { return description; }

    /** @return 是否有效（可售） */
    public boolean isActive() { return active; }
}
