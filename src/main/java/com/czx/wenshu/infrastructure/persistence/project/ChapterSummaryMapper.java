package com.czx.wenshu.infrastructure.persistence.project;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChapterSummaryMapper {

    @Select("""
            SELECT id, chapter_id, project_id, summary, created_at
            FROM chapter_summaries WHERE chapter_id = CAST(#{chapterId} AS UUID)
            """)
    ChapterSummaryRecord findByChapterId(@Param("chapterId") String chapterId);

    @Insert("""
            INSERT INTO chapter_summaries (id, chapter_id, project_id, summary, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{chapterId} AS UUID), CAST(#{projectId} AS UUID),
                    #{summary}, #{createdAt})
            """)
    void insert(ChapterSummaryRecord record);

    @Update("""
            UPDATE chapter_summaries SET summary = #{summary}, created_at = #{createdAt}
            WHERE chapter_id = CAST(#{chapterId} AS UUID)
            """)
    void update(ChapterSummaryRecord record);

    @Delete("DELETE FROM chapter_summaries WHERE chapter_id = CAST(#{chapterId} AS UUID)")
    void deleteByChapterId(@Param("chapterId") String chapterId);
}
