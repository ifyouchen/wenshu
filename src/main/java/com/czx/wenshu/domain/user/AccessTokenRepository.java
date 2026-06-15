package com.czx.wenshu.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface AccessTokenRepository {

    void save(AccessToken accessToken);

    Optional<AccessToken> findByTokenHash(String tokenHash);

    void revoke(UUID id, java.time.Instant revokedAt);

    void revokeAllForUser(UUID userId, java.time.Instant revokedAt);
}