package com.czx.wenshu.infrastructure.persistence.script;

import java.time.Instant;

/** MyBatis 持久化记录，对应 script_scenes 表（P7-01）。 */
public class ScriptSceneRecord {

    private String id;
    private String draftId;
    private String episodeId;
    private int sceneIndex;
    private String location;
    private String timeDesc;
    private Boolean interior;
    private String characters;
    private String content;
    private String sourceContent;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDraftId() { return draftId; }
    public void setDraftId(String draftId) { this.draftId = draftId; }
    public String getEpisodeId() { return episodeId; }
    public void setEpisodeId(String episodeId) { this.episodeId = episodeId; }
    public int getSceneIndex() { return sceneIndex; }
    public void setSceneIndex(int sceneIndex) { this.sceneIndex = sceneIndex; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTimeDesc() { return timeDesc; }
    public void setTimeDesc(String timeDesc) { this.timeDesc = timeDesc; }
    public Boolean getInterior() { return interior; }
    public void setInterior(Boolean interior) { this.interior = interior; }
    public String getCharacters() { return characters; }
    public void setCharacters(String characters) { this.characters = characters; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceContent() { return sourceContent; }
    public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
