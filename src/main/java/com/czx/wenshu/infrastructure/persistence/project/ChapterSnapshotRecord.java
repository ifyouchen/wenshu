package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;

public class ChapterSnapshotRecord {

    private String id;
    private String chapterId;
    private String content;
    private int wordCount;
    private String snapshotType;
    private String label;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public String getSnapshotType() { return snapshotType; }
    public void setSnapshotType(String snapshotType) { this.snapshotType = snapshotType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}