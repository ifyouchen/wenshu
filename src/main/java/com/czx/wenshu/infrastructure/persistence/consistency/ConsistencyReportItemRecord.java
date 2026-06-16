package com.czx.wenshu.infrastructure.persistence.consistency;

import java.time.Instant;

/** MyBatis 持久化记录，对应 consistency_report_items 表（P6-06/P6-07）。 */
public class ConsistencyReportItemRecord {

    private String id;
    private String reportId;
    private String projectId;
    private String type;
    private String character;
    private String chapterHint;
    private String description;
    private String suggestion;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getChapterHint() { return chapterHint; }
    public void setChapterHint(String chapterHint) { this.chapterHint = chapterHint; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
