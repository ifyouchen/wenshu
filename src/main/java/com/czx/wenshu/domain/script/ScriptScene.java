package com.czx.wenshu.domain.script;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 剧本场景实体（P7-01）。
 * 对应改编后的单个场景，包含场景剧本内容、原文引用和乐观锁版本号。
 */
public class ScriptScene {

    /** 场景唯一 ID。 */
    private final UUID id;
    /** 所属草稿 ID。 */
    private final UUID draftId;
    /** 所属集数 ID（可为 null 表示未分集）。 */
    private final UUID episodeId;
    /** 场景序号（从 0 开始，用于排序）。 */
    private final int sceneIndex;
    /** 场景地点描述。 */
    private String location;
    /** 时间描述（如"白天"/"黄昏"）。 */
    private String timeDesc;
    /** 是否室内场景。 */
    private Boolean interior;
    /** 涉及角色 JSON 字符串（数组格式）。 */
    private String characters;
    /** 改编后的剧本内容（包含台词/动作/旁白等）。 */
    private String content;
    /** 对应的原小说文本片段，用于对比。 */
    private String sourceContent;
    /**
     * 乐观锁版本号（P7-06），编辑时需携带当前版本，
     * 服务端版本不匹配则返回 409 CONFLICT。
     */
    private int version;
    /** 创建时间。 */
    private final Instant createdAt;
    /** 最后更新时间。 */
    private Instant updatedAt;

    private ScriptScene(UUID id, UUID draftId, UUID episodeId, int sceneIndex,
                         String location, String timeDesc, Boolean interior, String characters,
                         String content, String sourceContent, int version,
                         Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.draftId = Objects.requireNonNull(draftId, "draftId 不能为空");
        this.episodeId = episodeId;
        this.sceneIndex = sceneIndex;
        this.location = location;
        this.timeDesc = timeDesc;
        this.interior = interior;
        this.characters = characters != null ? characters : "[]";
        this.content = content;
        this.sourceContent = sourceContent;
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /**
     * 创建新场景。
     *
     * @param draftId       草稿 ID
     * @param sceneIndex    场景序号
     * @param sourceContent 原文片段
     * @param clock         时钟
     */
    public static ScriptScene create(UUID draftId, int sceneIndex, String sourceContent, Clock clock) {
        Instant now = Instant.now(clock);
        return new ScriptScene(UUID.randomUUID(), draftId, null, sceneIndex,
                null, null, null, "[]", null, sourceContent, 0, now, now);
    }

    /**
     * 从持久化记录重建。
     */
    public static ScriptScene rehydrate(UUID id, UUID draftId, UUID episodeId, int sceneIndex,
                                         String location, String timeDesc, Boolean interior,
                                         String characters, String content, String sourceContent,
                                         int version, Instant createdAt, Instant updatedAt) {
        return new ScriptScene(id, draftId, episodeId, sceneIndex, location, timeDesc,
                interior, characters, content, sourceContent, version, createdAt, updatedAt);
    }

    /**
     * 更新场景剧本内容（P7-05/P7-06）。
     * 乐观锁：若当前版本号与 expectedVersion 不匹配则抛出 {@link IllegalStateException}。
     *
     * @param content         新的剧本内容
     * @param location        场景地点
     * @param timeDesc        时间描述
     * @param expectedVersion 调用方持有的当前版本号
     * @param clock           时钟
     */
    public void updateContent(String content, String location, String timeDesc,
                               int expectedVersion, Clock clock) {
        if (this.version != expectedVersion) {
            throw new IllegalStateException(
                    "版本冲突：当前版本 " + this.version + "，提交版本 " + expectedVersion);
        }
        this.content = content;
        this.location = location;
        this.timeDesc = timeDesc;
        this.version++;
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID draftId() { return draftId; }
    public UUID episodeId() { return episodeId; }
    public int sceneIndex() { return sceneIndex; }
    public String location() { return location; }
    public String timeDesc() { return timeDesc; }
    public Boolean interior() { return interior; }
    public String characters() { return characters; }
    public String content() { return content; }
    public String sourceContent() { return sourceContent; }
    public int version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
