package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.EmailVerification;
import com.czx.wenshu.domain.user.EmailVerificationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisEmailVerificationRepository implements EmailVerificationRepository {

    private final EmailVerificationMapper mapper;

    public MyBatisEmailVerificationRepository(EmailVerificationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(EmailVerification verification) {
        mapper.insert(toRecord(verification));
    }

    @Override
    public Optional<EmailVerification> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash))
                .map(this::toDomain);
    }

    @Override
    public boolean existsUnusedCreatedAfter(UUID userId, Instant createdAfter) {
        return mapper.existsUnusedCreatedAfter(userId.toString(), createdAfter);
    }

    @Override
    public void markUsed(UUID id, Instant usedAt) {
        mapper.markUsed(id.toString(), usedAt);
    }

    private EmailVerification toDomain(EmailVerificationRecord record) {
        return EmailVerification.rehydrate(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getUserId()),
                record.getTokenHash(),
                record.getExpiresAt(),
                record.getUsedAt(),
                record.getCreatedAt()
        );
    }

    private EmailVerificationRecord toRecord(EmailVerification verification) {
        EmailVerificationRecord record = new EmailVerificationRecord();
        record.setId(verification.id().toString());
        record.setUserId(verification.userId().toString());
        record.setTokenHash(verification.tokenHash());
        record.setExpiresAt(verification.expiresAt());
        record.setUsedAt(verification.usedAt());
        record.setCreatedAt(verification.createdAt());
        return record;
    }
}
