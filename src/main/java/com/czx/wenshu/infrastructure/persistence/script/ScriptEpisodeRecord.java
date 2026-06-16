package com.czx.wenshu.infrastructure.persistence.script;

import java.time.Instant;

/** MyBatis 持久化记录，对应 script_episodes 表（P7-07）。 */
public class ScriptEpisodeRecord {

    private String id;
    private String draftId;
    private int episodeNo;
    private String title;
    private int sortOrder;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDraftId() { return draftId; }
    public void setDraftId(String draftId) { this.draftId = draftId; }
    public int getEpisodeNo() { return episodeNo; }
    public void setEpisodeNo(int episodeNo) { this.episodeNo = episodeNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
