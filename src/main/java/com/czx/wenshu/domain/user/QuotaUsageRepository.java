package com.czx.wenshu.domain.user;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户月度配额用量仓储端口（P6-05）。
 */
public interface QuotaUsageRepository {

    /**
     * 保存或更新配额记录（按 user_id + year_month UNIQUE 做 upsert）。
     *
     * @param quota 配额用量实体
     */
    void save(QuotaUsage quota);

    /**
     * 查询指定用户指定月份的配额记录。
     *
     * @param userId    用户 ID
     * @param yearMonth 月份（yyyy-MM）
     * @return 配额记录（Optional）
     */
    Optional<QuotaUsage> findByUserIdAndYearMonth(UUID userId, String yearMonth);
}
