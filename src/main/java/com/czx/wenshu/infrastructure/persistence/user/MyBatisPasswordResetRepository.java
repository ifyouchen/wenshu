package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.PasswordReset;
import com.czx.wenshu.domain.user.PasswordResetRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisPasswordResetRepository implements PasswordResetRepository {

    private final PasswordResetMapper mapper;

    public MyBatisPasswordResetRepository(PasswordResetMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordReset passwordReset) {
        mapper.insert(toRecord(passwordReset));
    }

    @Override
    public Optional<PasswordReset> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash))
                .map(this::toDomain);
    }

    @Override
    public void markUsed(UUID id, Instant usedAt) {
        mapper.markUsed(id.toString(), usedAt);
    }

    private PasswordReset toDomain(PasswordResetRecord record) {
        return PasswordReset.rehydrate(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getUserId()),
                record.getTokenHash(),
                record.getExpiresAt(),
                record.getUsedAt(),
                record.getCreatedAt()
        );
    }

    private PasswordResetRecord toRecord(PasswordReset passwordReset) {
        PasswordResetRecord record = new PasswordResetRecord();
        record.setId(passwordReset.id().toString());
        record.setUserId(passwordReset.userId().toString());
        record.setTokenHash(passwordReset.tokenHash());
        record.setExpiresAt(passwordReset.expiresAt());
        record.setUsedAt(passwordReset.usedAt());
        record.setCreatedAt(passwordReset.createdAt());
        return record;
    }
}
