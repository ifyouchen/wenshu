package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.UserSubscription;
import com.czx.wenshu.domain.user.UserSubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link UserSubscriptionRepository} 的 MyBatis 实现（P9-01）。
 */
@Repository
public class MyBatisUserSubscriptionRepository implements UserSubscriptionRepository {

    private final UserSubscriptionMapper mapper;

    public MyBatisUserSubscriptionRepository(UserSubscriptionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserSubscription> findActiveByUserId(UUID userId) {
        UserSubscriptionRecord rec = mapper.findActiveByUserId(userId.toString());
        return Optional.ofNullable(rec).map(this::toDomain);
    }

    @Override
    public void save(UserSubscription sub) {
        UserSubscriptionRecord rec = toRecord(sub);
        // 通过 findActiveByUserId 判断是否已存在，决定 insert 还是 update
        UserSubscriptionRecord existing = mapper.findActiveByUserId(sub.userId().toString());
        if (existing == null) {
            mapper.insert(rec);
        } else {
            mapper.update(rec);
        }
    }

    /** 领域对象转持久化记录。 */
    private UserSubscriptionRecord toRecord(UserSubscription sub) {
        UserSubscriptionRecord rec = new UserSubscriptionRecord();
        rec.setId(sub.id().toString());
        rec.setUserId(sub.userId().toString());
        rec.setPlanKey(sub.planKey());
        rec.setStartedAt(sub.startedAt());
        rec.setExpiresAt(sub.expiresAt());
        rec.setStatus(sub.status());
        return rec;
    }

    /** 持久化记录转领域对象。 */
    private UserSubscription toDomain(UserSubscriptionRecord rec) {
        return UserSubscription.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getUserId()),
                rec.getPlanKey(),
                rec.getStartedAt(),
                rec.getExpiresAt(),
                rec.getStatus()
        );
    }
}
