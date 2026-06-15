package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;

public class ProjectRecord {

    private String id;
    private String userId;
    private String title;
    private String genre;
    private String synopsis;
    private String worldview;
    private int totalWords;
    private int dailyCharGoal;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public String getWorldview() { return worldview; }
    public void setWorldview(String worldview) { this.worldview = worldview; }
    public int getTotalWords() { return totalWords; }
    public void setTotalWords(int totalWords) { this.totalWords = totalWords; }
    public int getDailyCharGoal() { return dailyCharGoal; }
    public void setDailyCharGoal(int dailyCharGoal) { this.dailyCharGoal = dailyCharGoal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}