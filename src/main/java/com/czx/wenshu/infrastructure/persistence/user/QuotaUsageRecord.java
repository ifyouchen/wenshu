package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;

/** MyBatis 持久化记录，对应 quota_usage 表（P6-05）。 */
public class QuotaUsageRecord {

    private String id;
    private String userId;
    private String yearMonth;
    private long usedChars;
    private int usedAdaptations;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public long getUsedChars() { return usedChars; }
    public void setUsedChars(long usedChars) { this.usedChars = usedChars; }
    public int getUsedAdaptations() { return usedAdaptations; }
    public void setUsedAdaptations(int usedAdaptations) { this.usedAdaptations = usedAdaptations; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
