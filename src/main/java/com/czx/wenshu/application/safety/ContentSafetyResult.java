package com.czx.wenshu.application.safety;

/**
 * 内容安全检测结果（P9-05）。
 *
 * <p>供 AI 输出流程调用：{@code passed=true} 表示内容可直接展示；
 * {@code passed=false} 时 {@code reason} 说明具体违规类型，
 * 前端展示替换说明并提供申诉入口。</p>
 */
public record ContentSafetyResult(
        /** true 表示内容安全可展示；false 表示已被过滤。 */
        boolean passed,
        /** 过滤原因（仅 passed=false 时有效，如 "含违禁词汇"）。 */
        String reason,
        /** 违规类型分类（如 "SPAM" / "VIOLENCE" / "POLITICAL"）。 */
        String category) {

    /**
     * 构造安全通过结果的快捷方法。
     *
     * @return 通过的检测结果
     */
    public static ContentSafetyResult safe() {
        return new ContentSafetyResult(true, null, null);
    }

    /**
     * 构造拦截结果的快捷方法。
     *
     * @param reason   过滤原因
     * @param category 违规类型
     * @return 拦截的检测结果
     */
    public static ContentSafetyResult unsafe(String reason, String category) {
        return new ContentSafetyResult(false, reason, category);
    }
}
