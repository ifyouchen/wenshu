package com.czx.wenshu.domain.script;

import java.util.List;
import java.util.UUID;

/**
 * 剧本集数仓储端口（P7-07）。
 */
public interface ScriptEpisodeRepository {

    /** 保存或更新集数。 */
    void save(ScriptEpisode episode);

    /** 按草稿 ID 查询所有集数，按 sort_order 正序。 */
    List<ScriptEpisode> findByDraftId(UUID draftId);

    /** 删除指定集数。 */
    void deleteById(UUID id);
}
