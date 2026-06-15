package com.czx.wenshu.domain.stats;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WritingDailyStatsRepository {

    WritingDailyStats save(WritingDailyStats stats);

    Optional<WritingDailyStats> findByUserIdAndProjectIdAndStatDate(UUID userId, UUID projectId, LocalDate statDate);

    void deleteById(UUID id);
}