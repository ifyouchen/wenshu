package com.czx.wenshu.infrastructure.persistence.script;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** MyBatis Mapper，对应 script_scenes 表（P7-01）。 */
@Mapper
public interface ScriptSceneMapper {

    @Select("""
            SELECT id, draft_id, episode_id, scene_index, location, time_desc, is_interior AS interior,
                   characters, content, source_content, version, created_at, updated_at
            FROM script_scenes WHERE id = CAST(#{id} AS UUID)
            """)
    ScriptSceneRecord findById(@Param("id") String id);

    @Select("""
            SELECT id, draft_id, episode_id, scene_index, location, time_desc, is_interior AS interior,
                   characters, content, source_content, version, created_at, updated_at
            FROM script_scenes WHERE draft_id = CAST(#{draftId} AS UUID)
            ORDER BY scene_index LIMIT #{limit} OFFSET #{offset}
            """)
    List<ScriptSceneRecord> findByDraftId(@Param("draftId") String draftId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM script_scenes WHERE draft_id = CAST(#{draftId} AS UUID)")
    int countByDraftId(@Param("draftId") String draftId);

    @Insert("""
            INSERT INTO script_scenes (id, draft_id, episode_id, scene_index, location, time_desc,
                is_interior, characters, content, source_content, version, created_at, updated_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{draftId} AS UUID), CAST(#{episodeId} AS UUID),
                    #{sceneIndex}, #{location}, #{timeDesc}, #{interior}, #{characters},
                    #{content}, #{sourceContent}, #{version}, #{createdAt}, #{updatedAt})
            """)
    void insert(ScriptSceneRecord record);

    @Update("""
            UPDATE script_scenes SET location=#{location}, time_desc=#{timeDesc},
                content=#{content}, version=#{version}, updated_at=#{updatedAt}
            WHERE id = CAST(#{id} AS UUID)
            """)
    void update(ScriptSceneRecord record);
}
