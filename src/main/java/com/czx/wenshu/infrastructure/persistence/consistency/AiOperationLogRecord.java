package com.czx.wenshu.infrastructure.persistence.consistency;

import java.time.Instant;

/** MyBatis 持久化记录，对应 ai_operation_logs 表（P6-06）。 */
public class AiOperationLogRecord {

    private String id;
    private String userId;
    private String projectId;
    private String operation;
    private String model;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
