package com.czx.wenshu.application.user;

import java.time.Instant;

/**
 * 当前用户订阅详情 DTO（P9-01/P9-02）。
 * 供 GET /subscriptions/current 接口返回，含套餐信息和当月用量。
 */
public record CurrentSubscriptionInfo(
        /** 套餐标识键。 */
        String planKey,
        /** 套餐名称。 */
        String planName,
        /** 订阅状态（active / cancelled / expired）。 */
        String status,
        /** 订阅到期时间（免费版为 null）。 */
        Instant expiresAt,
        /** 当月配额详情（含用量和限额）。 */
        QuotaInfo quota) {
}
