package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldElementRepository {

    WorldElement save(WorldElement element);

    Optional<WorldElement> findById(UUID id);

    List<WorldElement> findByProjectId(UUID projectId);

    /** P3-05：按项目 ID 和名称精确查找，用于角色名同步词典。 */
    Optional<WorldElement> findByProjectIdAndName(UUID projectId, String name);

    void deleteById(UUID id);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);
}