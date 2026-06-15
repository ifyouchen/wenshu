package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorldElementRepository {

    WorldElement save(WorldElement element);

    Optional<WorldElement> findById(UUID id);

    List<WorldElement> findByProjectId(UUID projectId);

    void deleteById(UUID id);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);
}