package com.czx.wenshu.domain.script;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 剧本集数实体（P7-07）。
 * 一个草稿可拆分为多集，每集包含若干场景。
 */
public class ScriptEpisode {

    /** 集数唯一 ID。 */
    private final UUID id;
    /** 所属草稿 ID。 */
    private final UUID draftId;
    /** 集数序号（从 1 开始）。 */
    private final int episodeNo;
    /** 集标题。 */
    private String title;
    /** 显示排序（允许手动调整）。 */
    private int sortOrder;
    /** 创建时间。 */
    private final Instant createdAt;

    private ScriptEpisode(UUID id, UUID draftId, int episodeNo, String title,
                           int sortOrder, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.draftId = Objects.requireNonNull(draftId, "draftId 不能为空");
        this.episodeNo = episodeNo;
        this.title = title;
        this.sortOrder = sortOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
    }

    /**
     * 创建新集数。
     *
     * @param draftId   草稿 ID
     * @param episodeNo 集序号
     * @param title     集标题
     * @param sortOrder 排序
     * @param clock     时钟
     */
    public static ScriptEpisode create(UUID draftId, int episodeNo, String title,
                                        int sortOrder, Clock clock) {
        return new ScriptEpisode(UUID.randomUUID(), draftId, episodeNo, title,
                sortOrder, Instant.now(clock));
    }

    /**
     * 从持久化记录重建。
     */
    public static ScriptEpisode rehydrate(UUID id, UUID draftId, int episodeNo,
                                           String title, int sortOrder, Instant createdAt) {
        return new ScriptEpisode(id, draftId, episodeNo, title, sortOrder, createdAt);
    }

    public UUID id() { return id; }
    public UUID draftId() { return draftId; }
    public int episodeNo() { return episodeNo; }
    public String title() { return title; }
    public int sortOrder() { return sortOrder; }
    public Instant createdAt() { return createdAt; }
}
