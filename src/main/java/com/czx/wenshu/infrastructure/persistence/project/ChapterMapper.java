package com.czx.wenshu.infrastructure.persistence.project;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface ChapterMapper {

    @Select("""
            SELECT id, volume_id, project_id, title, outline, content, word_count, sort_order, status, created_at, updated_at
            FROM chapters WHERE id = CAST(#{id} AS UUID)
            """)
    ChapterRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, volume_id, project_id, title, outline, content, word_count, sort_order, status, created_at, updated_at
            FROM chapters WHERE volume_id = CAST(#{volumeId} AS UUID)
            ORDER BY sort_order
            """)
    List<ChapterRecord> findByVolumeId(@Param("volumeId") String volumeId);

    @Select("""
            SELECT id, volume_id, project_id, title, outline, content, word_count, sort_order, status, created_at, updated_at
            FROM chapters WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY volume_id, sort_order
            """)
    List<ChapterRecord> findByProjectId(@Param("projectId") String projectId);

    @Select("""
            SELECT COUNT(1) FROM chapters WHERE id = CAST(#{id} AS UUID) AND project_id = CAST(#{projectId} AS UUID)
            """)
    boolean existsByIdAndProjectId(@Param("id") String id, @Param("projectId") String projectId);

    @Insert("""
            INSERT INTO chapters (id, volume_id, project_id, title, outline, content, word_count, sort_order, status, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{volumeId} AS UUID), CAST(#{projectId} AS UUID), #{title}, #{outline}, #{content}, #{wordCount}, #{sortOrder}, #{status}, #{createdAt}, #{updatedAt})
            """)
    void insert(ChapterRecord record);

    @Update("""
            UPDATE chapters SET title = #{title}, outline = #{outline}, content = #{content},
                word_count = #{wordCount}, sort_order = #{sortOrder}, status = #{status}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(ChapterRecord record);

    @Delete("DELETE FROM chapters WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}