package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetRepository {

    void save(PasswordReset passwordReset);

    Optional<PasswordReset> findByTokenHash(String tokenHash);

    void markUsed(UUID id, Instant usedAt);
}
