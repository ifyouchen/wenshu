package com.czx.wenshu.infrastructure.persistence.imports;

import java.time.Instant;

public class ImportParseSessionRecord {

    private String id;
    private String projectId;
    private String userId;
    private String parsedChapters;
    private Instant expiresAt;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getParsedChapters() { return parsedChapters; }
    public void setParsedChapters(String parsedChapters) { this.parsedChapters = parsedChapters; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
