package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.RegistrationEmailCode;
import com.czx.wenshu.domain.user.RegistrationEmailCodeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisRegistrationEmailCodeRepository implements RegistrationEmailCodeRepository {

    private final RegistrationEmailCodeMapper mapper;

    public MyBatisRegistrationEmailCodeRepository(RegistrationEmailCodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(RegistrationEmailCode code) {
        mapper.insert(toRecord(code));
    }

    @Override
    public Optional<RegistrationEmailCode> findLatestByEmailAndCodeHash(EmailAddress email, String codeHash) {
        return Optional.ofNullable(mapper.findLatestByEmailAndCodeHash(email.value(), codeHash))
                .map(this::toDomain);
    }

    @Override
    public boolean existsUnusedCreatedAfter(EmailAddress email, Instant createdAfter) {
        return mapper.existsUnusedCreatedAfter(email.value(), createdAfter);
    }

    @Override
    public void markUsed(UUID id, Instant usedAt) {
        mapper.markUsed(id.toString(), usedAt);
    }

    private RegistrationEmailCode toDomain(RegistrationEmailCodeRecord record) {
        return RegistrationEmailCode.rehydrate(
                UUID.fromString(record.getId()),
                record.getEmail(),
                record.getCodeHash(),
                record.getExpiresAt(),
                record.getUsedAt(),
                record.getCreatedAt()
        );
    }

    private RegistrationEmailCodeRecord toRecord(RegistrationEmailCode code) {
        RegistrationEmailCodeRecord record = new RegistrationEmailCodeRecord();
        record.setId(code.id().toString());
        record.setEmail(code.email().value());
        record.setCodeHash(code.codeHash());
        record.setExpiresAt(code.expiresAt());
        record.setUsedAt(code.usedAt());
        record.setCreatedAt(code.createdAt());
        return record;
    }
}
