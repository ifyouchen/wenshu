package com.czx.wenshu.infrastructure.persistence.script;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 script_drafts 表（P7-01）。 */
@Mapper
public interface ScriptDraftMapper {

    @Select("""
            SELECT id, project_id, user_id, title, strategy, status, total_scenes, created_at, updated_at
            FROM script_drafts WHERE id = CAST(#{id} AS UUID)
            """)
    ScriptDraftRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, project_id, user_id, title, strategy, status, total_scenes, created_at, updated_at
            FROM script_drafts WHERE project_id = CAST(#{projectId} AS UUID)
            ORDER BY created_at DESC
            """)
    List<ScriptDraftRecord> findByProjectId(@Param("projectId") String projectId);

    @Insert("""
            INSERT INTO script_drafts (id, project_id, user_id, title, strategy, status, total_scenes, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{projectId} AS UUID), CAST(#{userId} AS UUID),
                    #{title}, #{strategy}, #{status}, #{totalScenes}, #{createdAt}, #{updatedAt})
            """)
    void insert(ScriptDraftRecord record);

    @Update("""
            UPDATE script_drafts SET title=#{title}, strategy=#{strategy}, status=#{status},
                total_scenes=#{totalScenes}, updated_at=#{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(ScriptDraftRecord record);
}
