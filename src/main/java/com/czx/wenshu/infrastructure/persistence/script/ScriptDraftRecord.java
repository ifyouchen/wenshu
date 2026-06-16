package com.czx.wenshu.infrastructure.persistence.script;

import java.time.Instant;

/** MyBatis 持久化记录，对应 script_drafts 表（P7-01）。 */
public class ScriptDraftRecord {

    private String id;
    private String projectId;
    private String userId;
    private String title;
    private String strategy;
    private String status;
    private Integer totalScenes;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalScenes() { return totalScenes; }
    public void setTotalScenes(Integer totalScenes) { this.totalScenes = totalScenes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
