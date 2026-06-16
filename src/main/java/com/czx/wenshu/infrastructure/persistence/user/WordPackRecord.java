package com.czx.wenshu.infrastructure.persistence.user;

import java.time.Instant;

/** MyBatis 持久化记录，对应 word_packs 表（P9-09）。 */
public class WordPackRecord {

    private String id;
    private String userId;
    private String packKey;
    private String packType;
    private long charsTotal;
    private long charsUsed;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPackKey() { return packKey; }
    public void setPackKey(String packKey) { this.packKey = packKey; }
    public String getPackType() { return packType; }
    public void setPackType(String packType) { this.packType = packType; }
    public long getCharsTotal() { return charsTotal; }
    public void setCharsTotal(long charsTotal) { this.charsTotal = charsTotal; }
    public long getCharsUsed() { return charsUsed; }
    public void setCharsUsed(long charsUsed) { this.charsUsed = charsUsed; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
