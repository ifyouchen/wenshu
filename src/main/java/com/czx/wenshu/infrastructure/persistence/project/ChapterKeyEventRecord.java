package com.czx.wenshu.infrastructure.persistence.project;

import java.time.Instant;

/** MyBatis 持久化记录，对应 chapter_key_events 表（P6-03）。 */
public class ChapterKeyEventRecord {

    /** 记录 ID。 */
    private String id;
    /** 作品 ID。 */
    private String projectId;
    /** 章节 ID。 */
    private String chapterId;
    /** 事件文本。 */
    private String eventText;
    /** 事件类型。 */
    private String eventType;
    /** 涉及角色 JSON 字符串。 */
    private String characters;
    /** 重要程度。 */
    private double importance;
    /** 创建时间。 */
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }
    public String getEventText() { return eventText; }
    public void setEventText(String eventText) { this.eventText = eventText; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCharacters() { return characters; }
    public void setCharacters(String characters) { this.characters = characters; }
    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
