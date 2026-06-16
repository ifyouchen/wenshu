package com.czx.wenshu.infrastructure.persistence.project;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 chapter_key_events 表（P6-03）。 */
@Mapper
public interface ChapterKeyEventMapper {

    @Select("""
            SELECT id, project_id, chapter_id, event_text, event_type, characters, importance, created_at
            FROM chapter_key_events
            WHERE chapter_id = CAST(#{chapterId} AS UUID)
            ORDER BY importance DESC
            """)
    List<ChapterKeyEventRecord> findByChapterId(@Param("chapterId") String chapterId);

    @Select("""
            SELECT id, project_id, chapter_id, event_text, event_type, characters, importance, created_at
            FROM chapter_key_events
            WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY importance DESC
            """)
    List<ChapterKeyEventRecord> findByProjectId(@Param("projectId") String projectId);

    @Insert("""
            INSERT INTO chapter_key_events (id, project_id, chapter_id, event_text, event_type, characters, importance, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), CAST(#{chapterId} AS UUID),
                    #{eventText}, #{eventType}, #{characters}, #{importance}, #{createdAt})
            ON CONFLICT (chapter_id, event_text) DO NOTHING
            """)
    void insertIgnoreDuplicate(ChapterKeyEventRecord record);

    @Delete("DELETE FROM chapter_key_events WHERE chapter_id = CAST(#{chapterId} AS UUID)")
    void deleteByChapterId(@Param("chapterId") String chapterId);
}
