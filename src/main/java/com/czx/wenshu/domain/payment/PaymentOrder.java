package com.czx.wenshu.domain.payment;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 支付订单领域对象（P9-03）。
 *
 * <p>记录用户购买订阅套餐或字数包的支付生命周期。</p>
 *
 * <p>状态流转：</p>
 * <pre>
 *   pending → paid（支付成功）
 *          → cancelled（用户取消或超时）
 *   paid → refunded（退款，业务取消后）
 * </pre>
 */
public class PaymentOrder {

    /** 订单主键。 */
    private final UUID id;

    /** 下单用户 ID。 */
    private final UUID userId;

    /** 系统订单号（业务侧唯一，格式：WS{yyyyMMddHHmmss}{random8}）。 */
    private final String orderNo;

    /** 商品类型：{@code subscription}（订阅）/ {@code topup}（字数包）。 */
    private final String productType;

    /** 商品标识：plan_key 或字数包 key（如 {@code pro} / {@code topup_500k}）。 */
    private final String productKey;

    /** 支付金额（分，人民币）。 */
    private final long amountFen;

    /** 货币代码，默认 CNY。 */
    private final String currency;

    /** 支付渠道：wechat / alipay / credit_card（可为 null，下单时尚未确定）。 */
    private String paymentChannel;

    /** 订单状态：pending / paid / cancelled / refunded。 */
    private String status;

    /** 支付成功时间（支付完成后设置）。 */
    private Instant paidAt;

    /** 支付渠道返回的订单号（回调后填入）。 */
    private String channelOrderNo;

    /** 原始回调报文（用于事后核查）。 */
    private String rawCallback;

    /** 创建时间。 */
    private final Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 私有全参数构造器。 */
    private PaymentOrder(UUID id, UUID userId, String orderNo, String productType,
                         String productKey, long amountFen, String currency,
                         String paymentChannel, String status,
                         Instant paidAt, String channelOrderNo, String rawCallback,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.orderNo = orderNo;
        this.productType = productType;
        this.productKey = productKey;
        this.amountFen = amountFen;
        this.currency = currency;
        this.paymentChannel = paymentChannel;
        this.status = status;
        this.paidAt = paidAt;
        this.channelOrderNo = channelOrderNo;
        this.rawCallback = rawCallback;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建新支付订单（状态 pending）。
     *
     * @param userId      下单用户 ID
     * @param orderNo     系统订单号
     * @param productType 商品类型
     * @param productKey  商品标识
     * @param amountFen   金额（分）
     * @param clock       时钟
     * @return 新订单对象
     */
    public static PaymentOrder create(UUID userId, String orderNo, String productType,
                                      String productKey, long amountFen, Clock clock) {
        Instant now = clock.instant();
        return new PaymentOrder(UUID.randomUUID(), userId, orderNo, productType,
                productKey, amountFen, "CNY", null, "pending",
                null, null, null, now, now);
    }

    /**
     * 从持久层还原订单对象。
     */
    public static PaymentOrder rehydrate(UUID id, UUID userId, String orderNo,
                                         String productType, String productKey,
                                         long amountFen, String currency,
                                         String paymentChannel, String status,
                                         Instant paidAt, String channelOrderNo,
                                         String rawCallback,
                                         Instant createdAt, Instant updatedAt) {
        return new PaymentOrder(id, userId, orderNo, productType, productKey,
                amountFen, currency, paymentChannel, status, paidAt,
                channelOrderNo, rawCallback, createdAt, updatedAt);
    }

    /**
     * 标记订单为已支付（P9-03）。
     *
     * @param channelOrderNo 支付渠道订单号
     * @param rawCallback    原始回调报文
     * @param clock          时钟
     */
    public void markPaid(String channelOrderNo, String rawCallback, Clock clock) {
        this.status = "paid";
        this.channelOrderNo = channelOrderNo;
        this.rawCallback = rawCallback;
        this.paidAt = clock.instant();
        this.updatedAt = clock.instant();
    }

    /**
     * 标记订单为已取消（超时或用户主动取消）。
     *
     * @param clock 时钟
     */
    public void markCancelled(Clock clock) {
        this.status = "cancelled";
        this.updatedAt = clock.instant();
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** @return 订单主键 */
    public UUID id() { return id; }

    /** @return 下单用户 ID */
    public UUID userId() { return userId; }

    /** @return 系统订单号 */
    public String orderNo() { return orderNo; }

    /** @return 商品类型（subscription/topup） */
    public String productType() { return productType; }

    /** @return 商品标识 */
    public String productKey() { return productKey; }

    /** @return 金额（分） */
    public long amountFen() { return amountFen; }

    /** @return 货币代码 */
    public String currency() { return currency; }

    /** @return 支付渠道 */
    public String paymentChannel() { return paymentChannel; }

    /** @return 订单状态 */
    public String status() { return status; }

    /** @return 支付完成时间 */
    public Instant paidAt() { return paidAt; }

    /** @return 渠道订单号 */
    public String channelOrderNo() { return channelOrderNo; }

    /** @return 原始回调报文 */
    public String rawCallback() { return rawCallback; }

    /** @return 创建时间 */
    public Instant createdAt() { return createdAt; }

    /** @return 更新时间 */
    public Instant updatedAt() { return updatedAt; }
}
