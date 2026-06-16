package com.czx.wenshu.domain.project;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 章节关键事件（P6-03）。
 * 由 LLM 从章节内容中异步提取，记录情节转折、人物冲突、关键抉择等节点，
 * 用于一致性审查和向量检索。
 */
public class ChapterKeyEvent {

    /** 事件唯一 ID。 */
    private final UUID id;
    /** 所属作品 ID。 */
    private final UUID projectId;
    /** 所属章节 ID。 */
    private final UUID chapterId;
    /** 事件文本描述（最长 500 字符）。 */
    private final String eventText;
    /** 事件类型：conflict/relationship/turning_point/revelation/decision。 */
    private final String eventType;
    /** 涉及角色名称 JSON 数组字符串，例如 ["张三","李四"]。 */
    private final String characters;
    /** 重要程度，范围 0.0 ~ 1.0。 */
    private final double importance;
    /** 创建时间。 */
    private final Instant createdAt;

    private ChapterKeyEvent(UUID id, UUID projectId, UUID chapterId, String eventText,
                             String eventType, String characters, double importance, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.projectId = Objects.requireNonNull(projectId, "projectId 不能为空");
        this.chapterId = Objects.requireNonNull(chapterId, "chapterId 不能为空");
        this.eventText = Objects.requireNonNull(eventText, "eventText 不能为空");
        this.eventType = eventType;
        this.characters = characters != null ? characters : "[]";
        this.importance = Math.max(0.0, Math.min(1.0, importance));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
    }

    /**
     * 创建新的关键事件。
     *
     * @param projectId 所属作品 ID
     * @param chapterId 所属章节 ID
     * @param eventText 事件描述
     * @param eventType 事件类型
     * @param characters 涉及角色 JSON 字符串
     * @param importance 重要程度（0~1）
     * @param clock      时钟
     */
    public static ChapterKeyEvent create(UUID projectId, UUID chapterId, String eventText,
                                          String eventType, String characters, double importance,
                                          Clock clock) {
        return new ChapterKeyEvent(UUID.randomUUID(), projectId, chapterId, eventText,
                eventType, characters, importance, Instant.now(clock));
    }

    /**
     * 从持久化记录重建领域对象。
     */
    public static ChapterKeyEvent rehydrate(UUID id, UUID projectId, UUID chapterId, String eventText,
                                             String eventType, String characters, double importance,
                                             Instant createdAt) {
        return new ChapterKeyEvent(id, projectId, chapterId, eventText,
                eventType, characters, importance, createdAt);
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public UUID chapterId() { return chapterId; }
    public String eventText() { return eventText; }
    public String eventType() { return eventType; }
    public String characters() { return characters; }
    public double importance() { return importance; }
    public Instant createdAt() { return createdAt; }
}
