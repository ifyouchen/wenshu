package com.czx.wenshu.infrastructure.persistence.stats;

import com.czx.wenshu.domain.stats.WritingDailyStats;
import com.czx.wenshu.domain.stats.WritingDailyStatsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisWritingDailyStatsRepository implements WritingDailyStatsRepository {

    private final WritingDailyStatsMapper mapper;
    private final Clock clock;

    public MyBatisWritingDailyStatsRepository(WritingDailyStatsMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public WritingDailyStats save(WritingDailyStats stats) {
        WritingDailyStatsRecord record = toRecord(stats);
        WritingDailyStatsRecord existing = mapper.findByUserIdAndProjectIdAndStatDate(
                stats.userId().toString(), stats.projectId().toString(), stats.statDate().toString());
        if (existing == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
        return stats;
    }

    @Override
    public Optional<WritingDailyStats> findByUserIdAndProjectIdAndStatDate(UUID userId, UUID projectId, LocalDate statDate) {
        return Optional.ofNullable(mapper.findByUserIdAndProjectIdAndStatDate(
                userId.toString(), projectId != null ? projectId.toString() : null, statDate.toString()))
                .map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        throw new UnsupportedOperationException("Not needed yet");
    }

    private WritingDailyStats toDomain(WritingDailyStatsRecord r) {
        return WritingDailyStats.rehydrate(
                UUID.fromString(r.getId()), UUID.fromString(r.getUserId()),
                r.getProjectId() != null ? UUID.fromString(r.getProjectId()) : null,
                r.getStatDate(), r.getManualChars(), r.getAiAcceptedChars(), r.getTotalChars(), r.getUpdatedAt());
    }

    private WritingDailyStatsRecord toRecord(WritingDailyStats s) {
        WritingDailyStatsRecord r = new WritingDailyStatsRecord();
        r.setId(s.id().toString());
        r.setUserId(s.userId().toString());
        r.setProjectId(s.projectId() != null ? s.projectId().toString() : null);
        r.setStatDate(s.statDate());
        r.setManualChars(s.manualChars());
        r.setAiAcceptedChars(s.aiAcceptedChars());
        r.setTotalChars(s.totalChars());
        r.setUpdatedAt(s.updatedAt());
        return r;
    }
}