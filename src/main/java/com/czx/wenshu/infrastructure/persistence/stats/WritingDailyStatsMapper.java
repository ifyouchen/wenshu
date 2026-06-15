package com.czx.wenshu.infrastructure.persistence.stats;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WritingDailyStatsMapper {

    @Select("""
            SELECT id, user_id, project_id, stat_date, manual_chars, ai_accepted_chars, total_chars, updated_at
            FROM writing_daily_stats
            WHERE user_id = CAST(#{userId} AS UUID) AND project_id = CAST(#{projectId} AS UUID) AND stat_date = #{statDate}
            """)
    WritingDailyStatsRecord findByUserIdAndProjectIdAndStatDate(
            @Param("userId") String userId, @Param("projectId") String projectId, @Param("statDate") String statDate);

    @Insert("""
            INSERT INTO writing_daily_stats (id, user_id, project_id, stat_date, manual_chars, ai_accepted_chars, total_chars, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), CAST(#{projectId} AS UUID), #{statDate}, #{manualChars}, #{aiAcceptedChars}, #{totalChars}, #{updatedAt})
            """)
    void insert(WritingDailyStatsRecord record);

    @Update("""
            UPDATE writing_daily_stats
            SET manual_chars = #{manualChars}, ai_accepted_chars = #{aiAcceptedChars}, total_chars = #{totalChars}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(WritingDailyStatsRecord record);
}