package com.czx.wenshu.infrastructure.persistence.user;

import com.czx.wenshu.domain.user.SubscriptionPlan;
import com.czx.wenshu.domain.user.SubscriptionPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link SubscriptionPlanRepository} 的 MyBatis 实现（P9-02）。
 */
@Repository
public class MyBatisSubscriptionPlanRepository implements SubscriptionPlanRepository {

    private final SubscriptionPlanMapper mapper;

    public MyBatisSubscriptionPlanRepository(SubscriptionPlanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SubscriptionPlan> findAllActive() {
        return mapper.findAllActive().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SubscriptionPlan> findByPlanKey(String planKey) {
        SubscriptionPlanRecord rec = mapper.findByPlanKey(planKey);
        return Optional.ofNullable(rec).map(this::toDomain);
    }

    /** 将持久化记录转换为领域对象。 */
    private SubscriptionPlan toDomain(SubscriptionPlanRecord rec) {
        return SubscriptionPlan.rehydrate(
                UUID.fromString(rec.getId()),
                rec.getPlanKey(),
                rec.getName(),
                rec.getMonthlyCharLimit(),
                rec.getMonthlyAdaptationLimit(),
                rec.getPricePerMonth(),
                rec.getDescription(),
                rec.isActive()
        );
    }
}
