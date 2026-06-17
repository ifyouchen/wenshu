package com.czx.wenshu.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StyleTemplateRepository {

    StyleTemplate save(StyleTemplate template);

    Optional<StyleTemplate> findById(UUID id);

    List<StyleTemplate> findByUserId(UUID userId);

    List<StyleTemplate> findByUserIdAndType(UUID userId, String templateType);

    void deactivateByUserIdAndType(UUID userId, String templateType);

    void deleteById(UUID id);
}
