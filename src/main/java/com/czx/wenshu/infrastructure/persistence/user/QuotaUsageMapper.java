package com.czx.wenshu.infrastructure.persistence.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 quota_usage 表（P6-05）。 */
@Mapper
public interface QuotaUsageMapper {

    @Select("""
            SELECT id, user_id, year_month, used_chars, used_adaptations, updated_at
            FROM quota_usage
            WHERE user_id = CAST(#{userId} AS UUID) AND year_month = #{yearMonth}
            """)
    QuotaUsageRecord findByUserIdAndYearMonth(
            @Param("userId") String userId,
            @Param("yearMonth") String yearMonth);

    @Insert("""
            INSERT INTO quota_usage (id, user_id, year_month, used_chars, used_adaptations, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{yearMonth},
                    #{usedChars}, #{usedAdaptations}, #{updatedAt})
            """)
    void insert(QuotaUsageRecord record);

    @Update("""
            UPDATE quota_usage
            SET used_chars = #{usedChars}, used_adaptations = #{usedAdaptations}, updated_at = #{updatedAt}
            WHERE user_id = CAST(#{userId} AS UUID) AND year_month = #{yearMonth}
            """)
    void update(QuotaUsageRecord record);
}
