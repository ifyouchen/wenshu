package com.czx.wenshu.domain.user;

import java.util.List;
import java.util.Optional;

/**
 * 订阅套餐仓储端口（P9-02）。
 * 由基础设施层 MyBatis 实现。
 */
public interface SubscriptionPlanRepository {

    /**
     * 查询所有有效套餐（is_active = true），按月价格升序。
     *
     * @return 套餐列表
     */
    List<SubscriptionPlan> findAllActive();

    /**
     * 按套餐 key 查询套餐。
     *
     * @param planKey 套餐标识键
     * @return 套餐（可能不存在）
     */
    Optional<SubscriptionPlan> findByPlanKey(String planKey);
}
