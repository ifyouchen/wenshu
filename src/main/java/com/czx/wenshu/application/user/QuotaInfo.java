package com.czx.wenshu.application.user;

/**
 * 用户配额摘要响应 DTO（P6-05 / P9-09）。
 *
 * <p>包含当月已用量与套餐限额，以及字数包剩余量（P9-09 叠加）。</p>
 *
 * <p>前端展示规则：</p>
 * <ul>
 *   <li>月度配额 = {@code limitChars}，已用 = {@code usedChars}</li>
 *   <li>字数包余量 = {@code wordPackRemainingChars}（可单独展示，也可与月度剩余合并展示）</li>
 *   <li>总有效余量 = {@code remainingChars} + {@code wordPackRemainingChars}</li>
 * </ul>
 */
public record QuotaInfo(
        /** 当前统计月份（yyyy-MM）。 */
        String yearMonth,
        /** 已消耗 AI 操作字数（月度）。 */
        long usedChars,
        /** AI 操作字数月额度。 */
        long limitChars,
        /** 已消耗改编/审查次数（月度）。 */
        int usedAdaptations,
        /** 改编/审查次数月额度。 */
        int limitAdaptations,
        /** 月度剩余可用字数。 */
        long remainingChars,
        /** 剩余可用改编次数。 */
        int remainingAdaptations,
        /** 字数包剩余字符数（trial + topup 叠加，P9-09 新增）。 */
        long wordPackRemainingChars) {

    /**
     * 向后兼容构造器（不含字数包信息，字数包余量默认 0）。
     */
    public QuotaInfo(String yearMonth, long usedChars, long limitChars,
                     int usedAdaptations, int limitAdaptations,
                     long remainingChars, int remainingAdaptations) {
        this(yearMonth, usedChars, limitChars, usedAdaptations, limitAdaptations,
                remainingChars, remainingAdaptations, 0L);
    }

    /**
     * 总有效可用字符数（月度剩余 + 字数包剩余）。
     *
     * @return 综合可用字符数
     */
    public long totalEffectiveChars() {
        return remainingChars + wordPackRemainingChars;
    }
}
