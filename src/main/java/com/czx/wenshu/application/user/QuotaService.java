package com.czx.wenshu.application.user;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.user.QuotaUsage;
import com.czx.wenshu.domain.user.QuotaUsageRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户配额检查与扣减服务（P6-05）。
 * 免费套餐硬编码限额：每月 10 万 AI 字符，5 次改编/审查。
 * 后续可通过订阅表动态获取用户套餐限额。
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    /** 免费套餐：每月最大 AI 字符数。 */
    public static final long FREE_LIMIT_CHARS = 100_000L;
    /** 免费套餐：每月最大改编/一致性审查次数。 */
    public static final int FREE_LIMIT_ADAPTATIONS = 5;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final QuotaUsageRepository quotaUsageRepository;
    private final Clock clock;

    public QuotaService(QuotaUsageRepository quotaUsageRepository, Clock clock) {
        this.quotaUsageRepository = quotaUsageRepository;
        this.clock = clock;
    }

    /**
     * 获取用户当月配额详情（P6-05）。
     *
     * @param userId 用户 ID
     * @return 配额摘要
     */
    @Transactional
    public QuotaInfo getQuotaInfo(UUID userId) {
        String month = currentMonth();
        QuotaUsage usage = getOrCreateUsage(userId, month);
        log.debug("[QuotaService] 查询配额 userId={} month={} usedChars={} usedAdaptations={}",
                userId, month, usage.usedChars(), usage.usedAdaptations());
        return buildQuotaInfo(usage, month);
    }

    /**
     * 检查并扣减一次改编/一致性审查配额（P6-05）。
     * 超限时抛出 {@code ApiException(RATE_LIMITED)}。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void checkAndIncrementAdaptation(UUID userId) {
        String month = currentMonth();
        QuotaUsage usage = getOrCreateUsage(userId, month);
        if (usage.usedAdaptations() >= FREE_LIMIT_ADAPTATIONS) {
            log.warn("[QuotaService] 改编配额已耗尽 userId={} month={} used={}",
                    userId, month, usage.usedAdaptations());
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    String.format("本月改编/审查次数已达上限（%d 次），请升级套餐或等待下月重置。",
                            FREE_LIMIT_ADAPTATIONS));
        }
        usage.incrementAdaptations(clock);
        quotaUsageRepository.save(usage);
        log.info("[QuotaService] 改编配额扣减 userId={} month={} 剩余={}",
                userId, month, FREE_LIMIT_ADAPTATIONS - usage.usedAdaptations());
    }

    /**
     * 检查并扣减 AI 字符配额（P6-05）。
     * 超限时抛出 {@code ApiException(RATE_LIMITED)}。
     *
     * @param userId 用户 ID
     * @param chars  本次消耗字符数
     */
    @Transactional
    public void checkAndIncrementChars(UUID userId, long chars) {
        if (chars <= 0) return;
        String month = currentMonth();
        QuotaUsage usage = getOrCreateUsage(userId, month);
        if (usage.usedChars() + chars > FREE_LIMIT_CHARS) {
            log.warn("[QuotaService] AI 字符配额已耗尽 userId={} month={} used={} requested={}",
                    userId, month, usage.usedChars(), chars);
            throw new ApiException(ErrorCode.RATE_LIMITED,
                    String.format("本月 AI 字符额度已达上限（%d 字符），请升级套餐或等待下月重置。",
                            FREE_LIMIT_CHARS));
        }
        usage.incrementChars(chars, clock);
        quotaUsageRepository.save(usage);
        log.debug("[QuotaService] AI 字符配额扣减 userId={} month={} chars={} 已用={}",
                userId, month, chars, usage.usedChars());
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    /**
     * 获取或创建当月配额记录。
     */
    private QuotaUsage getOrCreateUsage(UUID userId, String month) {
        return quotaUsageRepository.findByUserIdAndYearMonth(userId, month)
                .orElseGet(() -> {
                    QuotaUsage newUsage = QuotaUsage.createForMonth(userId, month, clock);
                    quotaUsageRepository.save(newUsage);
                    return newUsage;
                });
    }

    /**
     * 获取当前年月字符串（yyyy-MM）。
     */
    private String currentMonth() {
        return LocalDate.now(clock).format(MONTH_FORMATTER);
    }

    /**
     * 构建配额摘要 DTO。
     */
    private QuotaInfo buildQuotaInfo(QuotaUsage usage, String month) {
        long remaining = Math.max(0, FREE_LIMIT_CHARS - usage.usedChars());
        int remainingAdaptations = Math.max(0, FREE_LIMIT_ADAPTATIONS - usage.usedAdaptations());
        return new QuotaInfo(month, usage.usedChars(), FREE_LIMIT_CHARS,
                usage.usedAdaptations(), FREE_LIMIT_ADAPTATIONS,
                remaining, remainingAdaptations);
    }
}
