package com.czx.wenshu.application.user;

import java.math.BigDecimal;

/**
 * 订阅套餐摘要 DTO（P9-02）。
 * 供 GET /subscriptions/plans 接口返回给前端。
 */
public record PlanInfo(
        /** 套餐标识键（free / pro / enterprise）。 */
        String planKey,
        /** 套餐名称。 */
        String name,
        /** 每月 AI 字符额度。 */
        long monthlyCharLimit,
        /** 每月改编/一致性审查次数额度。 */
        int monthlyAdaptationLimit,
        /** 每月价格（元），免费版为 0。 */
        BigDecimal pricePerMonth,
        /** 套餐描述。 */
        String description) {
}
