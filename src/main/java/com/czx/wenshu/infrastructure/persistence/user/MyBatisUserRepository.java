package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.EmailAddress;
import com.czx.wenshu.domain.user.IdentityType;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.domain.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserMapper userMapper;

    public MyBatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(userMapper.findById(id.toString()))
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return Optional.ofNullable(userMapper.findByEmail(email.value()))
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return userMapper.existsByEmail(email.value());
    }

    @Override
    public void save(User user) {
        UserRecord record = toRecord(user);
        if (userMapper.findById(user.id().toString()) == null) {
            userMapper.insert(record);
        } else {
            userMapper.update(record);
        }
    }

    private User toDomain(UserRecord record) {
        return User.rehydrate(
                UUID.fromString(record.getId()),
                record.getEmail(),
                record.getPasswordHash(),
                record.getNickname(),
                record.getAvatarUrl(),
                IdentityType.fromValue(record.getIdentityType()),
                record.isEmailVerified(),
                record.isAiTrainConsent(),
                record.getLoginFailCount(),
                record.getLockedUntil(),
                record.getLastLoginAt(),
                record.isDeleted(),
                record.getDeletedAt(),
                record.getDailyCharGoal(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private UserRecord toRecord(User user) {
        UserRecord record = new UserRecord();
        record.setId(user.id().toString());
        record.setEmail(user.email().value());
        record.setPasswordHash(user.passwordHash());
        record.setNickname(user.nickname());
        record.setAvatarUrl(user.avatarUrl());
        record.setIdentityType(user.identityType().value());
        record.setEmailVerified(user.isEmailVerified());
        record.setAiTrainConsent(user.isAiTrainConsent());
        record.setLoginFailCount(user.loginFailCount());
        record.setLockedUntil(user.lockedUntil());
        record.setLastLoginAt(user.lastLoginAt());
        record.setDeleted(user.isDeleted());
        record.setDeletedAt(user.deletedAt());
        record.setDailyCharGoal(user.dailyCharGoal());
        record.setCreatedAt(user.createdAt());
        record.setUpdatedAt(user.updatedAt());
        return record;
    }
}