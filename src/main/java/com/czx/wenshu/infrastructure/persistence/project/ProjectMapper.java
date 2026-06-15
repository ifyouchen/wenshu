package com.czx.wenshu.infrastructure.persistence.project;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface ProjectMapper {

    @Select("""
            SELECT id, user_id, title, genre, synopsis, worldview, total_words, daily_char_goal, status, created_at, updated_at
            FROM projects WHERE id = CAST(#{id} AS UUID)
            """)
    ProjectRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, user_id, title, genre, synopsis, worldview, total_words, daily_char_goal, status, created_at, updated_at
            FROM projects WHERE user_id = CAST(#{userId} AS UUID) AND status != 'deleted'
            ORDER BY updated_at DESC
            """)
    List<ProjectRecord> findByUserId(@Param("userId") String userId);

    @Select("""
            SELECT COUNT(1) FROM projects WHERE id = CAST(#{id} AS UUID) AND user_id = CAST(#{userId} AS UUID)
            """)
    boolean existsByIdAndUserId(@Param("id") String id, @Param("userId") String userId);

    @Insert("""
            INSERT INTO projects (id, user_id, title, genre, synopsis, worldview, total_words, daily_char_goal, status, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{userId} AS UUID), #{title}, #{genre}, #{synopsis}, #{worldview}, #{totalWords}, #{dailyCharGoal}, #{status}, #{createdAt}, #{updatedAt})
            """)
    void insert(ProjectRecord record);

    @Update("""
            UPDATE projects SET title = #{title}, genre = #{genre}, synopsis = #{synopsis}, worldview = #{worldview},
                total_words = #{totalWords}, daily_char_goal = #{dailyCharGoal}, status = #{status}, updated_at = #{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(ProjectRecord record);

    @Delete("DELETE FROM projects WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}