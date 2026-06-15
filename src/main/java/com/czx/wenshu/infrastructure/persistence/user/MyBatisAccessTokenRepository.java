package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.AccessToken;
import com.czx.wenshu.domain.user.AccessTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAccessTokenRepository implements AccessTokenRepository {

    private final AccessTokenMapper accessTokenMapper;

    public MyBatisAccessTokenRepository(AccessTokenMapper accessTokenMapper) {
        this.accessTokenMapper = accessTokenMapper;
    }

    @Override
    public void save(AccessToken accessToken) {
        accessTokenMapper.insert(toRecord(accessToken));
    }

    @Override
    public Optional<AccessToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(accessTokenMapper.findByTokenHash(tokenHash))
                .map(this::toDomain);
    }

    @Override
    public void revoke(UUID id, Instant revokedAt) {
        accessTokenMapper.revoke(id.toString(), revokedAt);
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant revokedAt) {
        accessTokenMapper.revokeAllForUser(userId.toString(), revokedAt);
    }

    private AccessToken toDomain(AccessTokenRecord record) {
        return AccessToken.rehydrate(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getUserId()),
                record.getTokenHash(),
                record.getExpiresAt(),
                record.getRevokedAt(),
                record.getCreatedAt()
        );
    }

    private AccessTokenRecord toRecord(AccessToken accessToken) {
        AccessTokenRecord record = new AccessTokenRecord();
        record.setId(accessToken.id().toString());
        record.setUserId(accessToken.userId().toString());
        record.setTokenHash(accessToken.tokenHash());
        record.setExpiresAt(accessToken.expiresAt());
        record.setRevokedAt(accessToken.revokedAt());
        record.setCreatedAt(accessToken.createdAt());
        return record;
    }
}