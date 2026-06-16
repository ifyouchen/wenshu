package com.czx.wenshu.infrastructure.persistence.user;

import java.math.BigDecimal;
import java.time.Instant;

/** MyBatis 持久化记录，对应 subscription_plans 表（P9-02）。 */
public class SubscriptionPlanRecord {

    private String id;
    private String planKey;
    private String name;
    private long monthlyCharLimit;
    private int monthlyAdaptationLimit;
    private BigDecimal pricePerMonth;
    private String description;
    private boolean active;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getMonthlyCharLimit() { return monthlyCharLimit; }
    public void setMonthlyCharLimit(long monthlyCharLimit) { this.monthlyCharLimit = monthlyCharLimit; }
    public int getMonthlyAdaptationLimit() { return monthlyAdaptationLimit; }
    public void setMonthlyAdaptationLimit(int monthlyAdaptationLimit) { this.monthlyAdaptationLimit = monthlyAdaptationLimit; }
    public BigDecimal getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(BigDecimal pricePerMonth) { this.pricePerMonth = pricePerMonth; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
