package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;

public class VolumeRecord {

    private String id;
    private String projectId;
    private String title;
    private String conflict;
    private int sortOrder;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getConflict() { return conflict; }
    public void setConflict(String conflict) { this.conflict = conflict; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}