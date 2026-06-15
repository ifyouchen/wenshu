package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.AccountRestoreToken;
import com.czx.wenshu.domain.user.AccountRestoreTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAccountRestoreTokenRepository implements AccountRestoreTokenRepository {

    private final AccountRestoreTokenMapper mapper;

    public MyBatisAccountRestoreTokenRepository(AccountRestoreTokenMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AccountRestoreToken token) {
        mapper.insert(toRecord(token));
    }

    @Override
    public Optional<AccountRestoreToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(mapper.findByTokenHash(tokenHash))
                .map(this::toDomain);
    }

    @Override
    public void markUsed(UUID id, Instant usedAt) {
        mapper.markUsed(id.toString(), usedAt);
    }

    private AccountRestoreToken toDomain(AccountRestoreTokenRecord record) {
        return AccountRestoreToken.rehydrate(
                UUID.fromString(record.getId()),
                UUID.fromString(record.getUserId()),
                record.getTokenHash(),
                record.getExpiresAt(),
                record.getUsedAt(),
                record.getCreatedAt()
        );
    }

    private AccountRestoreTokenRecord toRecord(AccountRestoreToken token) {
        AccountRestoreTokenRecord record = new AccountRestoreTokenRecord();
        record.setId(token.id().toString());
        record.setUserId(token.userId().toString());
        record.setTokenHash(token.tokenHash());
        record.setExpiresAt(token.expiresAt());
        record.setUsedAt(token.usedAt());
        record.setCreatedAt(token.createdAt());
        return record;
    }
}