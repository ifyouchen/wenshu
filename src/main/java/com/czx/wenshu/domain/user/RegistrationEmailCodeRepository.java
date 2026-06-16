package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationEmailCodeRepository {

    void save(RegistrationEmailCode code);

    Optional<RegistrationEmailCode> findLatestByEmailAndCodeHash(EmailAddress email, String codeHash);

    boolean existsUnusedCreatedAfter(EmailAddress email, Instant createdAfter);

    void markUsed(UUID id, Instant usedAt);
}
