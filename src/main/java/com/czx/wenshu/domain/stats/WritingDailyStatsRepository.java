package com.czx.wenshu.domain.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WritingDailyStatsRepository {

    WritingDailyStats save(WritingDailyStats stats);

    Optional<WritingDailyStats> findByUserIdAndProjectIdAndStatDate(UUID userId, UUID projectId, LocalDate statDate);

    /** P4-07/P4-08：查询用户在指定日期范围内的写作记录（含跨作品汇总用）。 */
    List<WritingDailyStats> findByUserIdAndStatDateBetween(UUID userId, LocalDate from, LocalDate to);

    void deleteById(UUID id);
}