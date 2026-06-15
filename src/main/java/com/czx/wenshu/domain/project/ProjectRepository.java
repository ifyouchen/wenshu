package com.czx.wenshu.domain.project;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(UUID id);

    java.util.List<Project> findByUserId(UUID userId);

    void deleteById(UUID id);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}