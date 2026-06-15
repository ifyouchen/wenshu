package com.czx.wenshu.application.stats;

import com.czx.wenshu.domain.stats.WritingDailyStats;
import com.czx.wenshu.domain.stats.WritingDailyStatsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WritingStatsService {

    private final WritingDailyStatsRepository statsRepository;
    private final Clock clock;

    public WritingStatsService(WritingDailyStatsRepository statsRepository, Clock clock) {
        this.statsRepository = statsRepository;
        this.clock = clock;
    }

    @Transactional
    public void recordManualDelta(UUID userId, UUID projectId, int delta) {
        if (delta <= 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        WritingDailyStats stats = statsRepository.findByUserIdAndProjectIdAndStatDate(userId, projectId, today)
                .orElseGet(() -> WritingDailyStats.create(userId, projectId, today, delta, clock));
        if (stats.id() != null && statsRepository.findByUserIdAndProjectIdAndStatDate(userId, projectId, today).isPresent()) {
            stats.addManualDelta(delta, clock);
        }
        statsRepository.save(stats);
    }
}