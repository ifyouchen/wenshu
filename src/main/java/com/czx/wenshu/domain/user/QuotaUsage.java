package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户月度配额用量记录（P6-05）。
 * 按月追踪 AI 操作字数与改编次数，用于免费套餐的限额检查。
 */
public class QuotaUsage {

    /** 记录唯一 ID。 */
    private final UUID id;
    /** 所属用户 ID。 */
    private final UUID userId;
    /** 统计月份，格式 yyyy-MM，例如 "2026-06"。 */
    private final String yearMonth;
    /** 已消耗 AI 操作字数。 */
    private long usedChars;
    /** 已消耗改编次数（包含一致性审查）。 */
    private int usedAdaptations;
    /** 最后更新时间。 */
    private Instant updatedAt;

    private QuotaUsage(UUID id, UUID userId, String yearMonth,
                        long usedChars, int usedAdaptations, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id 不能为空");
        this.userId = Objects.requireNonNull(userId, "userId 不能为空");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth 不能为空");
        this.usedChars = Math.max(0, usedChars);
        this.usedAdaptations = Math.max(0, usedAdaptations);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt 不能为空");
    }

    /**
     * 创建该月的新配额记录（初始用量均为 0）。
     *
     * @param userId    用户 ID
     * @param yearMonth 月份字符串（yyyy-MM）
     * @param clock     时钟
     */
    public static QuotaUsage createForMonth(UUID userId, String yearMonth, Clock clock) {
        return new QuotaUsage(UUID.randomUUID(), userId, yearMonth, 0, 0, Instant.now(clock));
    }

    /**
     * 从持久化记录重建领域对象。
     */
    public static QuotaUsage rehydrate(UUID id, UUID userId, String yearMonth,
                                        long usedChars, int usedAdaptations, Instant updatedAt) {
        return new QuotaUsage(id, userId, yearMonth, usedChars, usedAdaptations, updatedAt);
    }

    /**
     * 增加 AI 字数用量。
     *
     * @param chars  增量字数
     * @param clock  时钟
     */
    public void incrementChars(long chars, Clock clock) {
        this.usedChars += Math.max(0, chars);
        this.updatedAt = Instant.now(clock);
    }

    /**
     * 增加改编/审查次数用量（每次调用 +1）。
     *
     * @param clock 时钟
     */
    public void incrementAdaptations(Clock clock) {
        this.usedAdaptations++;
        this.updatedAt = Instant.now(clock);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String yearMonth() { return yearMonth; }
    public long usedChars() { return usedChars; }
    public int usedAdaptations() { return usedAdaptations; }
    public Instant updatedAt() { return updatedAt; }
}
