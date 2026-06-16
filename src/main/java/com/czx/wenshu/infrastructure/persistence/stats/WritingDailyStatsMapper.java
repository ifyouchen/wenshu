package com.czx.wenshu.infrastructure.persistence.stats;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 每日写作统计 MyBatis Mapper（P2-06 / P0-1 修复）。
 */
@Mapper
public interface WritingDailyStatsMapper {

    @Select("""
            SELECT id, user_id, project_id, stat_date, manual_chars, ai_accepted_chars,
                   total_chars, peak_hour, updated_at
            FROM writing_daily_stats
            WHERE user_id = CAST(#{userId} AS UUID) AND project_id = CAST(#{projectId} AS UUID) AND stat_date = #{statDate}
            """)
    WritingDailyStatsRecord findByUserIdAndProjectIdAndStatDate(
            @Param("userId") String userId, @Param("projectId") String projectId, @Param("statDate") String statDate);

    @Insert("""
            INSERT INTO writing_daily_stats (id, user_id, project_id, stat_date, manual_chars,
                   ai_accepted_chars, total_chars, peak_hour, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), CAST(#{projectId} AS UUID),
                   #{statDate}, #{manualChars}, #{aiAcceptedChars}, #{totalChars}, #{peakHour}, #{updatedAt})
            """)
    void insert(WritingDailyStatsRecord record);

    @Update("""
            UPDATE writing_daily_stats
            SET manual_chars = #{manualChars}, ai_accepted_chars = #{aiAcceptedChars},
                total_chars = #{totalChars}, peak_hour = #{peakHour}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(WritingDailyStatsRecord record);

    @Select("""
            SELECT id, user_id, project_id, stat_date, manual_chars, ai_accepted_chars,
                   total_chars, peak_hour, updated_at
            FROM writing_daily_stats
            WHERE user_id = CAST(#{userId} AS UUID) AND stat_date BETWEEN #{from} AND #{to}
            ORDER BY stat_date
            """)
    List<WritingDailyStatsRecord> findByUserIdAndStatDateBetween(
            @Param("userId") String userId, @Param("from") String from, @Param("to") String to);

    /**
     * 查询用户写作时间热力图数据（按星期和小时分组，P0-1 修复）。
     *
     * @param userId 用户 ID
     * @return 按 weekday（0=周日...6=周六）和 hour（0-23）分组的写作字数列表
     */
    @Select("""
            SELECT EXTRACT(DOW FROM stat_date)::INT AS weekday,
                   peak_hour AS hour,
                   SUM(total_chars) AS totalChars
            FROM writing_daily_stats
            WHERE user_id = CAST(#{userId} AS UUID)
              AND peak_hour >= 0
            GROUP BY EXTRACT(DOW FROM stat_date)::INT, peak_hour
            ORDER BY weekday, hour
            """)
    List<TimeHeatmapRecord> findTimeHeatmap(@Param("userId") String userId);

    /**
     * 时间热力图聚合数据记录（weekday × hour 维度）。
     */
    class TimeHeatmapRecord {
        private int weekday;
        private int hour;
        private long totalChars;

        /** @return 星期几（0=周日，1=周一，...，6=周六） */
        public int getWeekday() { return weekday; }

        /** @param weekday 星期几 */
        public void setWeekday(int weekday) { this.weekday = weekday; }

        /** @return 小时（0-23） */
        public int getHour() { return hour; }

        /** @param hour 小时 */
        public void setHour(int hour) { this.hour = hour; }

        /** @return 该时段总字数 */
        public long getTotalChars() { return totalChars; }

        /** @param totalChars 总字数 */
        public void setTotalChars(long totalChars) { this.totalChars = totalChars; }
    }
}
