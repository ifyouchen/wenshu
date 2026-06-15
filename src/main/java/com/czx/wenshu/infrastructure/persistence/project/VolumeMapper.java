package com.czx.wenshu.infrastructure.persistence.project;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface VolumeMapper {

    @Select("""
            SELECT id, project_id, title, conflict, sort_order, created_at
            FROM volumes WHERE id = CAST(#{id} AS UUID)
            """)
    VolumeRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, project_id, title, conflict, sort_order, created_at
            FROM volumes WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY sort_order
            """)
    List<VolumeRecord> findByProjectId(@Param("projectId") String projectId);

    @Select("SELECT COUNT(1) FROM volumes WHERE project_id = CAST(#{projectId} AS UUID)")
    int countByProjectId(@Param("projectId") String projectId);

    @Insert("""
            INSERT INTO volumes (id, project_id, title, conflict, sort_order, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), #{title}, #{conflict}, #{sortOrder}, #{createdAt})
            """)
    void insert(VolumeRecord record);

    @Update("""
            UPDATE volumes SET title = #{title}, conflict = #{conflict}, sort_order = #{sortOrder}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(VolumeRecord record);

    @Delete("DELETE FROM volumes WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}