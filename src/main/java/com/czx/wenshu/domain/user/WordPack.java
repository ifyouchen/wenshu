package com.czx.wenshu.domain.user;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 字数包领域对象（P9-09）。
 *
 * <p>字数包分两类：</p>
 * <ul>
 *   <li>{@code trial}：体验额度，注册时自动发放，免费且不过期</li>
 *   <li>{@code topup}：购买字数包，支付后发放，不过期（expires_at = null）</li>
 * </ul>
 *
 * <p>字数包不独立过期，与月度配额叠加：月度配额耗尽后自动消耗字数包。</p>
 */
public class WordPack {

    /** 体验额度包 key。 */
    public static final String TRIAL_KEY = "trial";
    /** 体验额度总字符数（5 万字）。 */
    public static final long TRIAL_CHARS = 50_000L;

    /** 字数包主键。 */
    private final UUID id;

    /** 所属用户 ID。 */
    private final UUID userId;

    /** 包标识（trial / topup_100k / topup_500k 等）。 */
    private final String packKey;

    /** 包类型：trial / topup。 */
    private final String packType;

    /** 总字符数。 */
    private final long charsTotal;

    /** 已消耗字符数。 */
    private long charsUsed;

    /** 到期时间（null = 永久有效）。 */
    private final Instant expiresAt;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 私有全参数构造器。 */
    private WordPack(UUID id, UUID userId, String packKey, String packType,
                     long charsTotal, long charsUsed,
                     Instant expiresAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.packKey = packKey;
        this.packType = packType;
        this.charsTotal = charsTotal;
        this.charsUsed = charsUsed;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新字数包（通用工厂方法）。
     *
     * @param userId     所属用户 ID
     * @param packKey    包标识
     * @param packType   包类型（trial/topup）
     * @param charsTotal 总字符数
     * @param clock      时钟
     * @return 新字数包对象
     */
    public static WordPack create(UUID userId, String packKey, String packType,
                                  long charsTotal, Clock clock) {
        Instant now = clock.instant();
        return new WordPack(UUID.randomUUID(), userId, packKey, packType,
                charsTotal, 0L, null, now, now);
    }

    /**
     * 为新注册用户发放体验额度（5 万字，不过期）。
     *
     * @param userId 新用户 ID
     * @param clock  时钟
     * @return 体验额度字数包
     */
    public static WordPack createTrial(UUID userId, Clock clock) {
        return create(userId, TRIAL_KEY, "trial", TRIAL_CHARS, clock);
    }

    /**
     * 从持久层还原字数包对象。
     */
    public static WordPack rehydrate(UUID id, UUID userId, String packKey, String packType,
                                     long charsTotal, long charsUsed,
                                     Instant expiresAt, Instant createdAt, Instant updatedAt) {
        return new WordPack(id, userId, packKey, packType, charsTotal, charsUsed,
                expiresAt, createdAt, updatedAt);
    }

    /**
     * 消耗指定字符数（P9-09）。
     * 不能超过剩余量，返回实际消耗数。
     *
     * @param chars 请求消耗字符数
     * @param clock 时钟
     * @return 实际消耗字符数（可能 < chars，当剩余不足时）
     */
    public long consume(long chars, Clock clock) {
        long remaining = charsTotal - charsUsed;
        long actual = Math.min(chars, remaining);
        this.charsUsed += actual;
        this.updatedAt = clock.instant();
        return actual;
    }

    /** @return 剩余字符数 */
    public long remainingChars() { return Math.max(0, charsTotal - charsUsed); }

    /** @return 是否已耗尽 */
    public boolean isExhausted() { return charsUsed >= charsTotal; }

    /**
     * 判断字数包是否有效（未过期 + 未耗尽）。
     *
     * @param clock 时钟
     * @return true 表示可继续使用
     */
    public boolean isEffective(Clock clock) {
        if (isExhausted()) return false;
        if (expiresAt == null) return true;
        return clock.instant().isBefore(expiresAt);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 主键 */
    public UUID id() { return id; }
    /** @return 用户 ID */
    public UUID userId() { return userId; }
    /** @return 包标识 */
    public String packKey() { return packKey; }
    /** @return 包类型 */
    public String packType() { return packType; }
    /** @return 总字符数 */
    public long charsTotal() { return charsTotal; }
    /** @return 已消耗字符数 */
    public long charsUsed() { return charsUsed; }
    /** @return 到期时间 */
    public Instant expiresAt() { return expiresAt; }
    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }
    /** @return 更新时间 */
    public Instant updatedAt() { return updatedAt; }
}
