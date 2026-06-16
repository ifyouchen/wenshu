package com.czx.wenshu.infrastructure.persistence.consistency;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 consistency_report_items 表（P6-06/P6-07）。 */
@Mapper
public interface ConsistencyReportItemMapper {

    @Select("""
            SELECT id, report_id, project_id, type, character, chapter_hint, description, suggestion,
                   status, created_at, updated_at
            FROM consistency_report_items
            WHERE id = CAST(#{id} AS UUID)
            """)
    ConsistencyReportItemRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, report_id, project_id, type, character, chapter_hint, description, suggestion,
                   status, created_at, updated_at
            FROM consistency_report_items
            WHERE report_id = CAST(#{reportId} AS UUID)
            ORDER BY created_at
            """)
    List<ConsistencyReportItemRecord> findByReportId(@Param("reportId") String reportId);

    @Insert("""
            INSERT INTO consistency_report_items
                (id, report_id, project_id, type, character, chapter_hint, description, suggestion,
                 status, created_at, updated_at)
            VALUES
                (CAST(#{id} AS UUID), CAST(#{reportId} AS UUID), CAST(#{projectId} AS UUID),
                 #{type}, #{character}, #{chapterHint}, #{description}, #{suggestion},
                 #{status}, #{createdAt}, #{updatedAt})
            """)
    void insert(ConsistencyReportItemRecord record);

    @Update("""
            UPDATE consistency_report_items
            SET status = #{status}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void updateStatus(ConsistencyReportItemRecord record);
}
