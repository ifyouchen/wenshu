package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountRestoreTokenRepository {

    void save(AccountRestoreToken token);

    Optional<AccountRestoreToken> findByTokenHash(String tokenHash);

    void markUsed(UUID id, Instant usedAt);
}