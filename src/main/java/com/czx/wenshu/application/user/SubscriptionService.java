package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.SubscriptionPlan;
import com.czx.wenshu.domain.user.SubscriptionPlanRepository;
import com.czx.wenshu.domain.user.UserSubscription;
import com.czx.wenshu.domain.user.UserSubscriptionRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅服务（P9-01/P9-02）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>查询系统所有有效套餐列表</li>
 *   <li>获取用户当前订阅及当月配额</li>
 *   <li>为 {@link QuotaService} 提供用户套餐限额（动态替代硬编码值）</li>
 * </ul>
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final QuotaService quotaService;
    private final Clock clock;

    public SubscriptionService(SubscriptionPlanRepository planRepository,
                               UserSubscriptionRepository subscriptionRepository,
                               QuotaService quotaService,
                               Clock clock) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.quotaService = quotaService;
        this.clock = clock;
    }

    /**
     * 查询所有对外有效的订阅套餐（P9-02）。
     *
     * @return 套餐列表（按价格升序）
     */
    public List<PlanInfo> getAvailablePlans() {
        List<SubscriptionPlan> plans = planRepository.findAllActive();
        log.debug("[SubscriptionService] 查询有效套餐列表，共 {} 个", plans.size());
        return plans.stream().map(this::toPlanInfo).toList();
    }

    /**
     * 获取用户当前订阅及当月配额详情（P9-01/P9-02）。
     *
     * <p>若用户无订阅记录，自动创建免费套餐订阅。</p>
     *
     * @param userId 用户 ID
     * @return 当前订阅信息（含配额用量）
     */
    @Transactional
    public CurrentSubscriptionInfo getCurrentSubscription(UUID userId) {
        UserSubscription sub = getOrCreateFreeSub(userId);
        SubscriptionPlan plan = planRepository.findByPlanKey(sub.planKey())
                .orElseGet(() -> planRepository.findByPlanKey(UserSubscription.FREE_PLAN_KEY)
                        .orElseThrow(() -> new IllegalStateException("免费套餐不存在，请检查数据库初始化")));

        log.debug("[SubscriptionService] 查询用户订阅 userId={} planKey={} status={}",
                userId, sub.planKey(), sub.status());

        QuotaInfo quota = quotaService.getQuotaInfoWithLimits(userId,
                plan.monthlyCharLimit(), plan.monthlyAdaptationLimit());

        return new CurrentSubscriptionInfo(
                plan.planKey(), plan.name(), sub.status(),
                sub.expiresAt(), quota);
    }

    /**
     * 获取用户当前套餐的配额限制（供 QuotaService 动态读取限额，P9-01）。
     *
     * <p>若用户无订阅记录，返回免费套餐限额。</p>
     *
     * @param userId 用户 ID
     * @return 套餐限额 [monthlyCharLimit, monthlyAdaptationLimit]
     */
    @Transactional
    public long[] getUserPlanLimits(UUID userId) {
        UserSubscription sub = getOrCreateFreeSub(userId);
        SubscriptionPlan plan = planRepository.findByPlanKey(sub.planKey())
                .orElseGet(() -> planRepository.findByPlanKey(UserSubscription.FREE_PLAN_KEY)
                        .orElse(null));
        if (plan == null) {
            // 降级：使用 QuotaService 硬编码免费额度
            return new long[]{QuotaService.FREE_LIMIT_CHARS, QuotaService.FREE_LIMIT_ADAPTATIONS};
        }
        return new long[]{plan.monthlyCharLimit(), plan.monthlyAdaptationLimit()};
    }

    // ── 私有辅助 ────────────────────────────────────────────────────────────

    /**
     * 获取用户有效订阅；若不存在则自动创建免费订阅记录。
     */
    private UserSubscription getOrCreateFreeSub(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId)
                .orElseGet(() -> {
                    log.info("[SubscriptionService] 用户 {} 无订阅记录，自动创建免费套餐", userId);
                    UserSubscription freeSub = UserSubscription.createFree(userId, clock);
                    subscriptionRepository.save(freeSub);
                    return freeSub;
                });
    }

    /** 将套餐领域对象转换为 DTO。 */
    private PlanInfo toPlanInfo(SubscriptionPlan plan) {
        return new PlanInfo(
                plan.planKey(), plan.name(),
                plan.monthlyCharLimit(), plan.monthlyAdaptationLimit(),
                plan.pricePerMonth(), plan.description());
    }
}
