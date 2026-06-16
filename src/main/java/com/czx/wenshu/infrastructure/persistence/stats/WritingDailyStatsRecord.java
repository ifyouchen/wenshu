package com.czx.wenshu.infrastructure.persistence.stats;

import java.time.Instant;
import java.time.LocalDate;

public class WritingDailyStatsRecord {

    private String id;
    private String userId;
    private String projectId;
    private LocalDate statDate;
    private int manualChars;
    private int aiAcceptedChars;
    private int totalChars;
    private int peakHour;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public int getManualChars() { return manualChars; }
    public void setManualChars(int manualChars) { this.manualChars = manualChars; }
    public int getAiAcceptedChars() { return aiAcceptedChars; }
    public void setAiAcceptedChars(int aiAcceptedChars) { this.aiAcceptedChars = aiAcceptedChars; }
    public int getTotalChars() { return totalChars; }
    public void setTotalChars(int totalChars) { this.totalChars = totalChars; }
    public int getPeakHour() { return peakHour; }
    public void setPeakHour(int peakHour) { this.peakHour = peakHour; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}