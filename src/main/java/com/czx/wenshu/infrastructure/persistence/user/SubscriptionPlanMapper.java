package com.czx.wenshu.infrastructure.persistence.user;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 subscription_plans 表（P9-02）。 */
@Mapper
public interface SubscriptionPlanMapper {

    /** 查询所有有效套餐，按月价格升序。 */
    @Select("""
            SELECT id, plan_key, name,
                   monthly_char_limit, monthly_adaptation_limit,
                   price_per_month, description,
                   is_active AS active, created_at
            FROM subscription_plans
            WHERE is_active = TRUE
            ORDER BY price_per_month ASC
            """)
    List<SubscriptionPlanRecord> findAllActive();

    /** 按套餐 key 查询。 */
    @Select("""
            SELECT id, plan_key, name,
                   monthly_char_limit, monthly_adaptation_limit,
                   price_per_month, description,
                   is_active AS active, created_at
            FROM subscription_plans
            WHERE plan_key = #{planKey}
            """)
    SubscriptionPlanRecord findByPlanKey(@Param("planKey") String planKey);
}
