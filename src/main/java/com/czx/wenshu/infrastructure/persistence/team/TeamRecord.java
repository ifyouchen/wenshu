package com.czx.wenshu.infrastructure.persistence.team;

import java.time.Instant;

/** MyBatis 持久化记录，对应 teams 表（P9-07）。 */
public class TeamRecord {

    private String id;
    private String ownerId;
    private String name;
    private String planKey;
    private long monthlyCharLimit;
    private int monthlyAdaptationLimit;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public long getMonthlyCharLimit() { return monthlyCharLimit; }
    public void setMonthlyCharLimit(long monthlyCharLimit) { this.monthlyCharLimit = monthlyCharLimit; }
    public int getMonthlyAdaptationLimit() { return monthlyAdaptationLimit; }
    public void setMonthlyAdaptationLimit(int monthlyAdaptationLimit) { this.monthlyAdaptationLimit = monthlyAdaptationLimit; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
