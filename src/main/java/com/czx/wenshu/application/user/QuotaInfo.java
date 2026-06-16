package com.czx.wenshu.application.user;

/**
 * 用户配额摘要响应 DTO（P6-05）。
 * 包含当月已用量与套餐限额，供前端展示配额 Tooltip 使用。
 */
public record QuotaInfo(
        /** 当前统计月份（yyyy-MM）。 */
        String yearMonth,
        /** 已消耗 AI 操作字数。 */
        long usedChars,
        /** AI 操作字数月额度（免费套餐 100000）。 */
        long limitChars,
        /** 已消耗改编/审查次数。 */
        int usedAdaptations,
        /** 改编/审查次数月额度（免费套餐 5）。 */
        int limitAdaptations,
        /** 剩余可用字数。 */
        long remainingChars,
        /** 剩余可用改编次数。 */
        int remainingAdaptations) {
}
