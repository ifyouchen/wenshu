package com.czx.wenshu.interfaces.rest.subscription;

import com.czx.wenshu.application.payment.OrderInfo;
import com.czx.wenshu.application.payment.PaymentService;
import com.czx.wenshu.application.user.CurrentSubscriptionInfo;
import com.czx.wenshu.application.user.PlanInfo;
import com.czx.wenshu.application.user.SubscriptionService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订阅套餐与支付接口（P9-02 / P9-03）。
 *
 * <ul>
 *   <li>GET  /subscriptions/plans    — 套餐列表（无需鉴权）</li>
 *   <li>GET  /subscriptions/current  — 当前订阅与配额（需鉴权）</li>
 *   <li>POST /subscriptions/checkout — 创建订阅购买订单（P9-03，需鉴权）</li>
 *   <li>POST /subscriptions/topup    — 创建字数包充值订单（P9-03，需鉴权）</li>
 *   <li>POST /subscriptions/cancel   — 取消自动续费（P9-03，需鉴权）</li>
 * </ul>
 */
@Tag(name = "Subscription", description = "订阅套餐、支付与当前订阅查询")
@Validated
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  PaymentService paymentService,
                                  CurrentUserProvider currentUserProvider) {
        this.subscriptionService = subscriptionService;
        this.paymentService = paymentService;
        this.currentUserProvider = currentUserProvider;
    }

    // ── 套餐查询（P9-02）────────────────────────────────────────────────────

    /** 查询所有有效订阅套餐（无需鉴权）。 */
    @Operation(summary = "查询订阅套餐列表（P9-02）")
    @GetMapping("/plans")
    public Result<List<PlanInfo>> getPlans() {
        List<PlanInfo> plans = subscriptionService.getAvailablePlans();
        log.info("[SubscriptionController] 查询套餐列表，共 {} 个", plans.size());
        return Result.ok(plans);
    }

    /** 获取当前用户订阅及配额详情（需鉴权）。 */
    @Operation(summary = "获取当前用户订阅与配额（P9-02）")
    @GetMapping("/current")
    public Result<CurrentSubscriptionInfo> getCurrentSubscription() {
        User user = currentUserProvider.getCurrentUser();
        log.info("[SubscriptionController] 查询用户订阅 userId={}", user.id());
        return Result.ok(subscriptionService.getCurrentSubscription(user.id()));
    }

    // ── 支付接口（P9-03）───────────────────────────────────────────────────

    /**
     * 创建订阅购买订单（P9-03）。
     *
     * <p>返回含 {@code payUrl} 的订单信息，前端跳转到支付页面。
     * 未配置支付渠道凭据时 payUrl = "PAYMENT_NOT_CONFIGURED:{orderNo}"。</p>
     */
    @Operation(summary = "创建订阅购买订单（P9-03）",
               description = "返回 payUrl，前端跳转完成支付后通过轮询订单状态确认。")
    @PostMapping("/checkout")
    public Result<OrderInfo> checkout(@Valid @RequestBody CheckoutRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[SubscriptionController] 用户 {} 发起订阅购买 planKey={}", user.id(), req.planKey());
        return Result.ok(paymentService.createCheckout(user.id(), req.planKey(), req.channel()));
    }

    /**
     * 创建字数包充值订单（P9-03）。
     */
    @Operation(summary = "创建字数包充值订单（P9-03）")
    @PostMapping("/topup")
    public Result<OrderInfo> topup(@Valid @RequestBody TopupRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[SubscriptionController] 用户 {} 发起字数包充值 topupKey={}", user.id(), req.topupKey());
        return Result.ok(paymentService.createTopup(user.id(), req.topupKey(), req.channel()));
    }

    /**
     * 取消自动续费（P9-03）。
     */
    @Operation(summary = "取消自动续费（P9-03）")
    @PostMapping("/cancel")
    public Result<Map<String, String>> cancel() {
        User user = currentUserProvider.getCurrentUser();
        String result = paymentService.cancelSubscription(user.id());
        log.info("[SubscriptionController] 用户 {} 取消续费 result={}", user.id(), result);
        return Result.ok(Map.of("status", result));
    }

    // ── 请求 DTO ──────────────────────────────────────────────────────────────

    /** 订阅购买请求。 */
    public record CheckoutRequest(
            @NotBlank(message = "套餐标识不能为空") String planKey,
            String channel) {
    }

    /** 字数包充值请求。 */
    public record TopupRequest(
            @NotBlank(message = "字数包标识不能为空") String topupKey,
            String channel) {
    }
}
