package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository {

    void save(EmailVerification verification);

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    boolean existsUnusedCreatedAfter(UUID userId, Instant createdAfter);

    void markUsed(UUID id, Instant usedAt);
}
