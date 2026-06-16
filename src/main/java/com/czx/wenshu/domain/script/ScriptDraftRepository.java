package com.czx.wenshu.domain.script;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 剧本草稿仓储端口（P7-01）。
 */
public interface ScriptDraftRepository {

    /** 保存或更新草稿。 */
    void save(ScriptDraft draft);

    /** 按 ID 查询草稿。 */
    Optional<ScriptDraft> findById(UUID id);

    /** 查询指定作品的所有草稿，按创建时间倒序。 */
    List<ScriptDraft> findByProjectId(UUID projectId);
}
