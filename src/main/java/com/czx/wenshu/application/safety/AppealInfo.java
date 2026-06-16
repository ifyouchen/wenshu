package com.czx.wenshu.application.safety;

import com.czx.wenshu.domain.safety.ContentAppeal;
import java.time.Instant;
import java.util.UUID;

/**
 * 内容安全申诉摘要 DTO（P9-05）。
 * 供 GET /content/appeals 接口返回给前端。
 */
public record AppealInfo(
        /** 申诉 ID。 */
        UUID id,
        /** 被过滤的内容片段（截断版）。 */
        String content,
        /** 申诉理由。 */
        String reason,
        /** 审核状态（pending / approved / rejected）。 */
        String status,
        /** 审核备注（可为 null）。 */
        String reviewerNote,
        /** 创建时间。 */
        Instant createdAt) {

    /** 从领域对象转换为 DTO。 */
    public static AppealInfo from(ContentAppeal appeal) {
        return new AppealInfo(
                appeal.id(),
                appeal.content(),
                appeal.reason(),
                appeal.status(),
                appeal.reviewerNote(),
                appeal.createdAt());
    }
}
