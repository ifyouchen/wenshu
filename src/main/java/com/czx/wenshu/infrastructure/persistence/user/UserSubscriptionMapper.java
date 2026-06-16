package com.czx.wenshu.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 user_subscriptions 表（P9-01）。 */
@Mapper
public interface UserSubscriptionMapper {

    /** 查询用户当前有效订阅记录（active 或 cancelled 状态）。 */
    @Select("""
            SELECT id, user_id, plan_key, started_at, expires_at, status, created_at, updated_at
            FROM user_subscriptions
            WHERE user_id = CAST(#{userId} AS UUID)
              AND status IN ('active', 'cancelled')
            ORDER BY started_at DESC
            LIMIT 1
            """)
    UserSubscriptionRecord findActiveByUserId(@Param("userId") String userId);

    /** 插入新订阅记录。 */
    @Insert("""
            INSERT INTO user_subscriptions
              (id, user_id, plan_key, started_at, expires_at, status, created_at, updated_at)
            VALUES
              (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{planKey},
               #{startedAt}, #{expiresAt}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    void insert(UserSubscriptionRecord record);

    /** 更新订阅状态（如取消续费）。 */
    @Update("""
            UPDATE user_subscriptions
            SET status = #{status}, updated_at = CURRENT_TIMESTAMP
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(UserSubscriptionRecord record);
}
