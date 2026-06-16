package com.czx.wenshu.infrastructure.persistence.project;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorldElementMapper {

    @Select("""
            SELECT id, project_id, type, name, description, aliases, is_locked AS locked, created_at
            FROM world_elements WHERE id = CAST(#{id} AS UUID)
            """)
    WorldElementRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, project_id, type, name, description, aliases, is_locked AS locked, created_at
            FROM world_elements WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY created_at
            """)
    List<WorldElementRecord> findByProjectId(@Param("projectId") String projectId);

    @Select("""
            SELECT id, project_id, type, name, description, aliases, is_locked AS locked, created_at
            FROM world_elements WHERE project_id = CAST(#{projectId} AS UUID) AND name = #{name}
            LIMIT 1
            """)
    WorldElementRecord findByProjectIdAndName(@Param("projectId") String projectId, @Param("name") String name);

    @Select("""
            SELECT COUNT(1) FROM world_elements WHERE id = CAST(#{id} AS UUID) AND project_id = CAST(#{projectId} AS UUID)
            """)
    boolean existsByIdAndProjectId(@Param("id") String id, @Param("projectId") String projectId);

    /**
     * 统计指定作品的词典条目数（P1-5：容量限制检查）。
     *
     * @param projectId 作品 ID
     * @return 条目数量
     */
    @Select("SELECT COUNT(1) FROM world_elements WHERE project_id = CAST(#{projectId} AS UUID)")
    int countByProjectId(@Param("projectId") String projectId);

    @Insert("""
            INSERT INTO world_elements (id, project_id, type, name, description, aliases, is_locked, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), #{type}, #{name}, #{description}, #{aliases}, #{locked}, #{createdAt})
            """)
    void insert(WorldElementRecord record);

    @Update("""
            UPDATE world_elements SET type = #{type}, name = #{name}, description = #{description},
                aliases = #{aliases}, is_locked = #{locked}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(WorldElementRecord record);

    @Delete("DELETE FROM world_elements WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}