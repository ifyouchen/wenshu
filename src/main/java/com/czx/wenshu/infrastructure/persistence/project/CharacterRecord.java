package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;

public class CharacterRecord {

    private String id;
    private String projectId;
    private String name;
    private String role;
    private String appearance;
    private String personality;
    private String abilities;
    private String speechStyle;
    private String status;
    private boolean locked;
    private String firstChapterId;
    private String lastActiveChapterId;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public String getAbilities() { return abilities; }
    public void setAbilities(String abilities) { this.abilities = abilities; }
    public String getSpeechStyle() { return speechStyle; }
    public void setSpeechStyle(String speechStyle) { this.speechStyle = speechStyle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public String getFirstChapterId() { return firstChapterId; }
    public void setFirstChapterId(String firstChapterId) { this.firstChapterId = firstChapterId; }
    public String getLastActiveChapterId() { return lastActiveChapterId; }
    public void setLastActiveChapterId(String lastActiveChapterId) { this.lastActiveChapterId = lastActiveChapterId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}