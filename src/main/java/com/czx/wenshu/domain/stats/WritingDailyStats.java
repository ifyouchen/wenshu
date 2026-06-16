package com.czx.wenshu.domain.stats;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * 每日写作统计领域对象。
 *
 * <p>记录用户在某一天针对某个作品的写作字数，包括手动输入字数、AI 接受字数和
 * 高峰写作小时（peakHour，-1 表示未记录）。</p>
 */
public class WritingDailyStats {

    private UUID id;
    private final UUID userId;
    private UUID projectId;
    private LocalDate statDate;
    private int manualChars;
    private int aiAcceptedChars;
    private int totalChars;
    /** 当日写作高峰小时（0-23），-1 表示未记录。 */
    private int peakHour;
    private Instant updatedAt;

    private WritingDailyStats(UUID id, UUID userId, UUID projectId, LocalDate statDate,
                              int manualChars, int aiAcceptedChars, int totalChars,
                              int peakHour, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.projectId = projectId;
        this.statDate = Objects.requireNonNull(statDate, "statDate must not be null");
        this.manualChars = manualChars;
        this.aiAcceptedChars = aiAcceptedChars;
        this.totalChars = totalChars;
        this.peakHour = peakHour;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * 创建新的每日统计记录（peakHour 初始为 -1，表示未记录）。
     *
     * @param userId    用户 ID
     * @param projectId 作品 ID
     * @param statDate  统计日期
     * @param delta     初始字数差量
     * @param clock     时钟
     * @return 新的统计记录
     */
    public static WritingDailyStats create(UUID userId, UUID projectId, LocalDate statDate, int delta, Clock clock) {
        int safeDelta = Math.max(delta, 0);
        return new WritingDailyStats(UUID.randomUUID(), userId, projectId, statDate,
                safeDelta, 0, safeDelta, -1, Instant.now(clock));
    }

    /**
     * 从持久层还原每日统计记录。
     *
     * @param id             记录 ID
     * @param userId         用户 ID
     * @param projectId      作品 ID
     * @param statDate       统计日期
     * @param manualChars    手动输入字数
     * @param aiAcceptedChars AI 接受字数
     * @param totalChars     总字数
     * @param peakHour       高峰小时（-1 表示未记录）
     * @param updatedAt      最后更新时间
     * @return 还原的统计记录
     */
    public static WritingDailyStats rehydrate(UUID id, UUID userId, UUID projectId, LocalDate statDate,
                                               int manualChars, int aiAcceptedChars, int totalChars,
                                               int peakHour, Instant updatedAt) {
        return new WritingDailyStats(id, userId, projectId, statDate,
                manualChars, aiAcceptedChars, totalChars, peakHour, updatedAt);
    }

    /**
     * 累加手动输入字数（P2-06：保存章节时调用）。
     *
     * @param delta 字数差量
     * @param clock 时钟
     */
    public void addManualDelta(int delta, Clock clock) {
        if (delta > 0) {
            this.manualChars += delta;
            this.totalChars += delta;
        }
        this.updatedAt = Instant.now(clock);
    }

    /**
     * 累加 AI 接受字数（P0-1 修复：用户接受 AI 生成内容时调用）。
     *
     * @param delta 被接受的 AI 字符数
     * @param clock 时钟
     */
    public void addAiAcceptedDelta(int delta, Clock clock) {
        if (delta > 0) {
            this.aiAcceptedChars += delta;
            this.totalChars += delta;
        }
        this.updatedAt = Instant.now(clock);
    }

    /**
     * 更新当日写作高峰小时（P0-1 修复：记录写作时间分布）。
     *
     * @param hour  写作小时（0-23）
     * @param clock 时钟
     */
    public void updatePeakHour(int hour, Clock clock) {
        this.peakHour = hour;
        this.updatedAt = Instant.now(clock);
    }

    /** @return 记录 ID */
    public UUID id() { return id; }

    /** @return 用户 ID */
    public UUID userId() { return userId; }

    /** @return 作品 ID */
    public UUID projectId() { return projectId; }

    /** @return 统计日期 */
    public LocalDate statDate() { return statDate; }

    /** @return 手动输入字数 */
    public int manualChars() { return manualChars; }

    /** @return AI 接受字数 */
    public int aiAcceptedChars() { return aiAcceptedChars; }

    /** @return 总字数 */
    public int totalChars() { return totalChars; }

    /** @return 高峰写作小时（-1 表示未记录） */
    public int peakHour() { return peakHour; }

    /** @return 最后更新时间 */
    public Instant updatedAt() { return updatedAt; }
}
