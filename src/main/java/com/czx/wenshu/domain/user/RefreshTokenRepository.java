package com.czx.wenshu.domain.user;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revoke(UUID id, Instant revokedAt, UUID replacedById);

    void revokeAllForUser(UUID userId, Instant revokedAt);
}
