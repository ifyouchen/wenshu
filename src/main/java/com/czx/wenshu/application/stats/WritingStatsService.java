package com.czx.wenshu.application.stats;

import com.czx.wenshu.domain.stats.WritingDailyStats;
import com.czx.wenshu.domain.stats.WritingDailyStatsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 写作统计记录服务（P2-06 / P0-1 / P1-1 修复）。 */
@Service
public class WritingStatsService {

    private static final Logger log = LoggerFactory.getLogger(WritingStatsService.class);

    private final WritingDailyStatsRepository statsRepository;
    private final Clock clock;

    /**
     * 构造写作统计服务。
     *
     * @param statsRepository 每日统计仓储
     * @param clock           时钟
     */
    public WritingStatsService(WritingDailyStatsRepository statsRepository, Clock clock) {
        this.statsRepository = statsRepository;
        this.clock = clock;
    }

    /** 记录手动输入字数差量（章节保存时调用）。 */
    @Transactional
    public void recordManualDelta(UUID userId, UUID projectId, int delta) {
        if (delta <= 0) return;
        LocalDate today = LocalDate.now(clock);
        WritingDailyStats stats = getOrCreate(userId, projectId, today, delta);
        if (stats.id() != null && statsRepository.findByUserIdAndProjectIdAndStatDate(userId, projectId, today).isPresent()) {
            stats.addManualDelta(delta, clock);
        }
        stats.updatePeakHour(LocalTime.now(clock).getHour(), clock);
        statsRepository.save(stats);
        log.debug("[WritingStatsService] 手动字数记录 userId={} delta={}", userId, delta);
    }

    /**
     * 记录 AI 接受字数（用户接受 AI 生成内容时调用，P0-1）。
     *
     * @param userId    用户 ID
     * @param projectId 作品 ID
     * @param chars     被接受的 AI 字符数
     */
    @Transactional
    public void recordAiAcceptedDelta(UUID userId, UUID projectId, int chars) {
        if (chars <= 0) return;
        LocalDate today = LocalDate.now(clock);
        WritingDailyStats stats = getOrCreate(userId, projectId, today, 0);
        stats.addAiAcceptedDelta(chars, clock);
        stats.updatePeakHour(LocalTime.now(clock).getHour(), clock);
        statsRepository.save(stats);
        log.info("[WritingStatsService] AI 接受字数记录 userId={} chars={}", userId, chars);
    }

    private WritingDailyStats getOrCreate(UUID userId, UUID projectId, LocalDate date, int initialDelta) {
        return statsRepository.findByUserIdAndProjectIdAndStatDate(userId, projectId, date)
                .orElseGet(() -> {
                    WritingDailyStats s = WritingDailyStats.create(userId, projectId, date, initialDelta, clock);
                    statsRepository.save(s);
                    return s;
                });
    }
}
