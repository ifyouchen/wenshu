package com.czx.wenshu.interfaces.rest.subscription;

import com.czx.wenshu.application.user.CurrentSubscriptionInfo;
import com.czx.wenshu.application.user.PlanInfo;
import com.czx.wenshu.application.user.SubscriptionService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订阅套餐接口（P9-02）。
 *
 * <ul>
 *   <li>{@code GET /api/v1/subscriptions/plans} — 查询系统所有可用套餐（无需鉴权）</li>
 *   <li>{@code GET /api/v1/subscriptions/current} — 当前用户订阅及配额（需鉴权）</li>
 * </ul>
 */
@Tag(name = "Subscription", description = "订阅套餐与用户当前订阅查询")
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  CurrentUserProvider currentUserProvider) {
        this.subscriptionService = subscriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 查询所有有效订阅套餐（P9-02）。
     * 无需鉴权，任何访客均可查看套餐页。
     *
     * @return 套餐列表（按月价格升序：免费版 → 专业版 → 企业版）
     */
    @Operation(summary = "查询订阅套餐列表")
    @GetMapping("/plans")
    public Result<List<PlanInfo>> getPlans() {
        List<PlanInfo> plans = subscriptionService.getAvailablePlans();
        log.info("[SubscriptionController] 查询套餐列表，共 {} 个", plans.size());
        return Result.ok(plans);
    }

    /**
     * 获取当前用户订阅及配额详情（P9-01/P9-02）。
     * 需要 Bearer Token 鉴权。若用户无订阅记录，自动创建免费套餐订阅。
     *
     * @return 当前订阅信息（套餐名称、状态、到期时间、当月配额用量）
     */
    @Operation(summary = "获取当前用户订阅与配额")
    @GetMapping("/current")
    public Result<CurrentSubscriptionInfo> getCurrentSubscription() {
        var user = currentUserProvider.getCurrentUser();
        log.info("[SubscriptionController] 查询用户订阅 userId={}", user.id());
        return Result.ok(subscriptionService.getCurrentSubscription(user.id()));
    }
}
