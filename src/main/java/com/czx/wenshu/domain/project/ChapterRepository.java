package com.czx.wenshu.domain.project;

import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository {

    Chapter save(Chapter chapter);

    Optional<Chapter> findById(UUID id);

    java.util.List<Chapter> findByVolumeId(UUID volumeId);

    void deleteById(UUID id);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);

    java.util.List<Chapter> findByProjectId(UUID projectId);
}