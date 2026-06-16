package com.czx.wenshu.domain.imports;

import java.util.Optional;
import java.util.UUID;

public interface ImportParseSessionRepository {

    void save(ImportParseSession session);

    Optional<ImportParseSession> findById(UUID id);

    void deleteById(UUID id);
}
