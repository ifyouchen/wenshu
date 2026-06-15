package com.czx.wenshu.infrastructure.persistence.project;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CharacterMapper {

    @Select("""
            SELECT id, project_id, name, role, appearance, personality, abilities, speech_style, status,
                   is_locked, first_chapter_id, last_active_chapter_id, created_at, updated_at
            FROM characters WHERE id = CAST(#{id} AS UUID)
            """)
    CharacterRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, project_id, name, role, appearance, personality, abilities, speech_style, status,
                   is_locked, first_chapter_id, last_active_chapter_id, created_at, updated_at
            FROM characters WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY created_at
            """)
    List<CharacterRecord> findByProjectId(@Param("projectId") String projectId);

    @Select("""
            SELECT COUNT(1) FROM characters WHERE id = CAST(#{id} AS UUID) AND project_id = CAST(#{projectId} AS UUID)
            """)
    boolean existsByIdAndProjectId(@Param("id") String id, @Param("projectId") String projectId);

    @Insert("""
            INSERT INTO characters (id, project_id, name, role, appearance, personality, abilities, speech_style,
                                    status, is_locked, first_chapter_id, last_active_chapter_id, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), #{name}, #{role}, #{appearance}, #{personality},
                    #{abilities}, #{speechStyle}, #{status}, #{locked},
                    CAST(#{firstChapterId} AS UUID), CAST(#{lastActiveChapterId} AS UUID), #{createdAt}, #{updatedAt})
            """)
    void insert(CharacterRecord record);

    @Update("""
            UPDATE characters SET name = #{name}, role = #{role}, appearance = #{appearance}, personality = #{personality},
                    abilities = #{abilities}, speech_style = #{speechStyle}, status = #{status},
                    is_locked = #{locked}, first_chapter_id = CAST(#{firstChapterId} AS UUID),
                    last_active_chapter_id = CAST(#{lastActiveChapterId} AS UUID), updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(CharacterRecord record);

    @Delete("DELETE FROM characters WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}