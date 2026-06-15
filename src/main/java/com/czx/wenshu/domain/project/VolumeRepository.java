package com.czx.wenshu.domain.project;

import java.util.Optional;
import java.util.UUID;

public interface VolumeRepository {

    Volume save(Volume volume);

    Optional<Volume> findById(UUID id);

    java.util.List<Volume> findByProjectId(UUID projectId);

    void deleteById(UUID id);

    int countByProjectId(UUID projectId);
}