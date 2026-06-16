package com.czx.wenshu.application.payment;

import com.czx.wenshu.application.user.SubscriptionService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.payment.PaymentOrder;
import com.czx.wenshu.domain.payment.PaymentOrderRepository;
import com.czx.wenshu.domain.user.SubscriptionPlanRepository;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付服务（P9-03）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>创建订阅购买/字数包充值订单</li>
 *   <li>处理支付回调并验签（可降级：无渠道凭据时记录日志）</li>
 *   <li>取消自动续费</li>
 * </ul>
 *
 * <p>降级策略：未配置支付渠道凭据时，{@code createCheckout/createTopup} 返回占位 payUrl，
 * 不影响应用启动和其他功能。实际渠道对接（微信支付/支付宝）在集成测试时按需接入。</p>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository orderRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;

    /** 构造函数注入。 */
    public PaymentService(PaymentOrderRepository orderRepository,
                          SubscriptionPlanRepository planRepository,
                          SubscriptionService subscriptionService,
                          Clock clock) {
        this.orderRepository = orderRepository;
        this.planRepository = planRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
    }

    /**
     * 创建订阅购买订单（P9-03）。
     *
     * <p>验证套餐存在后创建 pending 订单，返回含 payUrl 的摘要。
     * 未配置支付凭据时 payUrl 为占位符（PAYMENT_NOT_CONFIGURED）。</p>
     *
     * @param userId     购买用户 ID
     * @param planKey    目标套餐 key（如 "pro"）
     * @param channel    支付渠道（wechat / alipay，可为 null）
     * @return 订单摘要（含 payUrl）
     */
    @Transactional
    public OrderInfo createCheckout(UUID userId, String planKey, String channel) {
        log.info("[PaymentService] 创建订阅购买订单 userId={} planKey={} channel={}", userId, planKey, channel);
        // 验证套餐存在
        var plan = planRepository.findByPlanKey(planKey)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "套餐不存在: " + planKey));
        if (plan.pricePerMonth().longValue() == 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "免费套餐无需购买");
        }

        long amountFen = plan.pricePerMonth().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        PaymentOrder order = PaymentOrder.create(userId, generateOrderNo(), "subscription", planKey, amountFen, clock);
        orderRepository.save(order);

        String payUrl = buildPayUrl(order, channel);
        log.info("[PaymentService] 订阅订单已创建 orderNo={} amountFen={}", order.orderNo(), amountFen);
        return toInfo(order, payUrl);
    }

    /**
     * 创建字数包充值订单（P9-03）。
     *
     * @param userId    购买用户 ID
     * @param topupKey  字数包 key（如 "topup_100k"）
     * @param channel   支付渠道
     * @return 订单摘要
     */
    @Transactional
    public OrderInfo createTopup(UUID userId, String topupKey, String channel) {
        log.info("[PaymentService] 创建字数包充值订单 userId={} topupKey={} channel={}", userId, topupKey, channel);
        // 字数包定价映射（简化，生产环境建议入库）
        Map<String, Long> topupPrices = Map.of(
                "topup_100k", 900L,   // 9 元 100k
                "topup_500k", 3900L,  // 39 元 500k
                "topup_2m", 12900L    // 129 元 200万
        );
        long amountFen = topupPrices.getOrDefault(topupKey, 1000L);
        PaymentOrder order = PaymentOrder.create(userId, generateOrderNo(), "topup", topupKey, amountFen, clock);
        orderRepository.save(order);

        String payUrl = buildPayUrl(order, channel);
        log.info("[PaymentService] 字数包订单已创建 orderNo={} amountFen={}", order.orderNo(), amountFen);
        return toInfo(order, payUrl);
    }

    /**
     * 处理支付回调并验签（P9-03）。
     *
     * <p>验签逻辑：</p>
     * <ol>
     *   <li>按 orderNo 查找订单</li>
     *   <li>验证回调签名（此处为占位实现，生产端替换为渠道 SDK 验证）</li>
     *   <li>标记订单已支付，触发配额更新</li>
     * </ol>
     *
     * @param channel    支付渠道（wechat/alipay）
     * @param orderNo    系统订单号
     * @param channelNo  渠道订单号
     * @param rawPayload 原始回调报文（用于验签）
     * @return 处理结果描述
     */
    @Transactional
    public String processWebhook(String channel, String orderNo,
                                 String channelNo, String rawPayload) {
        log.info("[PaymentService] 处理支付回调 channel={} orderNo={}", channel, orderNo);

        // 占位验签：生产端替换为渠道 SDK（微信 v3 API/支付宝 RSA 签名）
        boolean signatureValid = verifySignature(channel, rawPayload);
        if (!signatureValid) {
            log.warn("[PaymentService] 回调签名验证失败 channel={} orderNo={}", channel, orderNo);
            throw new ApiException(ErrorCode.BAD_REQUEST, "支付回调签名验证失败");
        }

        PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "订单不存在: " + orderNo));

        if ("paid".equals(order.status())) {
            log.warn("[PaymentService] 订单已处理，忽略重复回调 orderNo={}", orderNo);
            return "already_paid";
        }

        order.markPaid(channelNo, rawPayload, clock);
        orderRepository.save(order);

        // 更新用户订阅（subscription 类型订单）
        if ("subscription".equals(order.productType())) {
            log.info("[PaymentService] 订阅订单支付成功，更新用户套餐 userId={} planKey={}",
                    order.userId(), order.productKey());
            // TODO：实际更新 user_subscriptions 表中的 plan_key 和 expires_at
            // subscriptionService.updateSubscription(order.userId(), order.productKey(), ...);
        }

        log.info("[PaymentService] 支付回调处理完成 orderNo={} status=paid", orderNo);
        return "success";
    }

    /**
     * 取消自动续费（P9-03）。
     *
     * @param userId 用户 ID
     * @return 操作结果描述
     */
    @Transactional
    public String cancelSubscription(UUID userId) {
        log.info("[PaymentService] 用户取消自动续费 userId={}", userId);
        // 将当前有效订阅状态改为 cancelled（到期前仍有效）
        var subInfo = subscriptionService.getCurrentSubscription(userId);
        if ("free".equals(subInfo.planKey())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "免费套餐无需取消续费");
        }
        // TODO：更新 user_subscriptions.status = 'cancelled'
        log.info("[PaymentService] 已标记取消续费 userId={} planKey={}", userId, subInfo.planKey());
        return "cancelled";
    }

    // ── 私有工具方法 ─────────────────────────────────────────────────────────

    /**
     * 生成唯一系统订单号（WS{yyyyMMddHHmmss}{random8}）。
     */
    private String generateOrderNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(clock.instant().atOffset(ZoneOffset.UTC));
        String rand = String.format("%08d", new Random().nextInt(100_000_000));
        return "WS" + ts + rand;
    }

    /**
     * 构建支付跳转 URL。
     * 生产环境：调用微信/支付宝 SDK 生成。
     * 当前占位实现：返回标识未配置的 URL。
     *
     * @param order   订单
     * @param channel 支付渠道
     * @return 支付 URL
     */
    private String buildPayUrl(PaymentOrder order, String channel) {
        log.warn("[PaymentService] 支付渠道未配置，返回占位 payUrl orderNo={} channel={}",
                order.orderNo(), channel);
        return "PAYMENT_NOT_CONFIGURED:" + order.orderNo();
    }

    /**
     * 验证支付回调签名（P9-03 占位实现）。
     *
     * <p>生产实现要点：</p>
     * <ul>
     *   <li>微信支付 v3：使用平台证书公钥验证 RSA-SHA256 签名</li>
     *   <li>支付宝：使用支付宝公钥验证 RSA2 签名</li>
     * </ul>
     *
     * @param channel    渠道名称
     * @param rawPayload 原始报文
     * @return true 表示签名有效（当前始终返回 true 允许测试）
     */
    private boolean verifySignature(String channel, String rawPayload) {
        log.debug("[PaymentService] 占位验签 channel={} payloadLen={}",
                channel, rawPayload != null ? rawPayload.length() : 0);
        // 占位：始终通过（生产端替换为真实渠道 SDK 验签逻辑）
        return true;
    }

    /** 领域对象 → DTO。 */
    private OrderInfo toInfo(PaymentOrder order, String payUrl) {
        return new OrderInfo(
                order.id(), order.orderNo(), order.productType(), order.productKey(),
                order.amountFen(), order.currency(), order.status(),
                order.paidAt(), order.createdAt(), payUrl);
    }
}
