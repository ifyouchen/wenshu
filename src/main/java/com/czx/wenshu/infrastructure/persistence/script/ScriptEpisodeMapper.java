package com.czx.wenshu.infrastructure.persistence.script;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** MyBatis Mapper，对应 script_episodes 表（P7-07）。 */
@Mapper
public interface ScriptEpisodeMapper {

    @Select("""
            SELECT id, draft_id, episode_no, title, sort_order, created_at
            FROM script_episodes WHERE draft_id = CAST(#{draftId} AS UUID) ORDER BY sort_order
            """)
    List<ScriptEpisodeRecord> findByDraftId(@Param("draftId") String draftId);

    @Insert("""
            INSERT INTO script_episodes (id, draft_id, episode_no, title, sort_order, created_at)
            VALUES (CAST(#{id} AS UUID), CAST(#{draftId} AS UUID), #{episodeNo}, #{title}, #{sortOrder}, #{createdAt})
            """)
    void insert(ScriptEpisodeRecord record);

    @Delete("DELETE FROM script_episodes WHERE id = CAST(#{id} AS UUID)")
    void deleteById(@Param("id") String id);
}
