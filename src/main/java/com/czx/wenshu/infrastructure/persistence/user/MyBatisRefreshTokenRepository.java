package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.RefreshToken;
import com.czx.wenshu.domain.user.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenMapper mapper;

    public MyBatisRefreshTokenRepository(RefreshTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(RefreshToken refreshToken) {
        mapper.insert(toRecord(refreshToken));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash))
                .map(this::toDomain);
    }

    @Override
    public void revoke(UUID id, Instant revokedAt, UUID replacedById) {
        mapper.revoke(id.toString(), revokedAt, replacedById == null ? null : replacedById.toString());
    }

    private RefreshToken toDomain(RefreshTokenRecord record) {
        return RefreshToken.rehydrate(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getUserId()),
                record.getTokenHash(),
                record.getExpiresAt(),
                record.getRevokedAt(),
                record.getReplacedById() == null ? null : UUID.fromString(record.getReplacedById()),
                record.getCreatedAt()
        );
    }

    private RefreshTokenRecord toRecord(RefreshToken refreshToken) {
        RefreshTokenRecord record = new RefreshTokenRecord();
        record.setId(refreshToken.id().toString());
        record.setUserId(refreshToken.userId().toString());
        record.setTokenHash(refreshToken.tokenHash());
        record.setExpiresAt(refreshToken.expiresAt());
        record.setRevokedAt(refreshToken.revokedAt());
        record.setReplacedById(refreshToken.replacedById() == null ? null : refreshToken.replacedById().toString());
        record.setCreatedAt(refreshToken.createdAt());
        return record;
    }
}
