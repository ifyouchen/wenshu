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
            SELECT id, project_id, type, name, description, is_locked AS locked, created_at
            FROM world_elements WHERE id = CAST(#{id} AS UUID)
            """)
    WorldElementRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, project_id, type, name, description, is_locked AS locked, created_at
            FROM world_elements WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY created_at
            """)
    List<WorldElementRecord> findByProjectId(@Param("projectId") String projectId);

    @Select("""
            SELECT COUNT(1) FROM world_elements WHERE id = CAST(#{id} AS UUID) AND project_id = CAST(#{projectId} AS UUID)
            """)
    boolean existsByIdAndProjectId(@Param("id") String id, @Param("projectId") String projectId);

    @Insert("""
            INSERT INTO world_elements (id, project_id, type, name, description, is_locked, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), #{type}, #{name}, #{description}, #{locked}, #{createdAt})
            """)
    void insert(WorldElementRecord record);

    @Update("""
            UPDATE world_elements SET type = #{type}, name = #{name}, description = #{description}, is_locked = #{locked}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(WorldElementRecord record);

    @Delete("DELETE FROM world_elements WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}