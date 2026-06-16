package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;

public class UserStyleProfileRecord {

    private String id;
    private String userId;
    private String sampleText;
    private String styleTags;
    private String analysisTaskId;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSampleText() { return sampleText; }
    public void setSampleText(String sampleText) { this.sampleText = sampleText; }
    public String getStyleTags() { return styleTags; }
    public void setStyleTags(String styleTags) { this.styleTags = styleTags; }
    public String getAnalysisTaskId() { return analysisTaskId; }
    public void setAnalysisTaskId(String analysisTaskId) { this.analysisTaskId = analysisTaskId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
