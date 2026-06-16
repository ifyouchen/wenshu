package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.QuotaUsage;
import com.czx.wenshu.domain.user.QuotaUsageRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的配额用量仓储（P6-05）。 */
@Repository
public class MyBatisQuotaUsageRepository implements QuotaUsageRepository {

    /** MyBatis Mapper。 */
    private final QuotaUsageMapper mapper;

    public MyBatisQuotaUsageRepository(QuotaUsageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(QuotaUsage quota) {
        QuotaUsageRecord record = toRecord(quota);
        if (mapper.findByUserIdAndYearMonth(quota.userId().toString(), quota.yearMonth()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
    }

    @Override
    public Optional<QuotaUsage> findByUserIdAndYearMonth(UUID userId, String yearMonth) {
        return Optional.ofNullable(mapper.findByUserIdAndYearMonth(userId.toString(), yearMonth))
                .map(this::toDomain);
    }

    private QuotaUsage toDomain(QuotaUsageRecord r) {
        return QuotaUsage.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getUserId()),
                r.getYearMonth(),
                r.getUsedChars(),
                r.getUsedAdaptations(),
                r.getUpdatedAt()
        );
    }

    private QuotaUsageRecord toRecord(QuotaUsage q) {
        QuotaUsageRecord r = new QuotaUsageRecord();
        r.setId(q.id().toString());
        r.setUserId(q.userId().toString());
        r.setYearMonth(q.yearMonth());
        r.setUsedChars(q.usedChars());
        r.setUsedAdaptations(q.usedAdaptations());
        r.setUpdatedAt(q.updatedAt());
        return r;
    }
}
