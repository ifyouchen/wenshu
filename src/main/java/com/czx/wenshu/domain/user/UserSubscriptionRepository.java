package com.czx.wenshu.domain.user;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户订阅仓储端口（P9-01）。
 * 由基础设施层 MyBatis 实现。
 */
public interface UserSubscriptionRepository {

    /**
     * 查询用户当前有效的订阅记录（status = 'active' 或 'cancelled'）。
     * 同一用户在任何时刻理论上只有一条有效记录。
     *
     * @param userId 用户 ID
     * @return 有效订阅（可能不存在，新用户首次查询时需创建）
     */
    Optional<UserSubscription> findActiveByUserId(UUID userId);

    /**
     * 保存或更新订阅记录（按主键 upsert）。
     *
     * @param subscription 要保存的订阅对象
     */
    void save(UserSubscription subscription);
}
