package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterRepository {

    Character save(Character character);

    Optional<Character> findById(UUID id);

    List<Character> findByProjectId(UUID projectId);

    void deleteById(UUID id);

    boolean existsByIdAndProjectId(UUID id, UUID projectId);
}