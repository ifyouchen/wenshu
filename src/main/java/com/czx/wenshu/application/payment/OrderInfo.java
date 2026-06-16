package com.czx.wenshu.application.payment;

import java.time.Instant;
import java.util.UUID;

/**
 * 支付订单摘要 DTO（P9-03）。
 * 供前端展示订单列表和发起支付使用。
 */
public record OrderInfo(
        /** 订单主键。 */
        UUID id,
        /** 系统订单号。 */
        String orderNo,
        /** 商品类型（subscription/topup）。 */
        String productType,
        /** 商品标识（如 pro/topup_500k）。 */
        String productKey,
        /** 金额（分）。 */
        long amountFen,
        /** 货币代码。 */
        String currency,
        /** 订单状态。 */
        String status,
        /** 支付完成时间（可为 null）。 */
        Instant paidAt,
        /** 创建时间。 */
        Instant createdAt,
        /**
         * 支付跳转 URL（pending 状态时有效，由支付渠道返回）。
         * COS 未配置时为占位 URL。
         */
        String payUrl) {
}
