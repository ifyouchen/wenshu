package com.czx.wenshu.domain.script;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 剧本场景仓储端口（P7-01）。
 */
public interface ScriptSceneRepository {

    /** 保存或更新场景。 */
    void save(ScriptScene scene);

    /** 按 ID 查询场景。 */
    Optional<ScriptScene> findById(UUID id);

    /**
     * 查询草稿的场景分页列表，按 scene_index 正序。
     *
     * @param draftId 草稿 ID
     * @param offset  偏移量
     * @param limit   每页条数
     */
    List<ScriptScene> findByDraftId(UUID draftId, int offset, int limit);

    /** 查询草稿的场景总数。 */
    int countByDraftId(UUID draftId);
}
