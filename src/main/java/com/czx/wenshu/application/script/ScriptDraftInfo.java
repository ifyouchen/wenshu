package com.czx.wenshu.application.script;

import com.czx.wenshu.domain.script.ScriptDraft;

/**
 * 剧本草稿响应 DTO（P7-01）。
 */
public record ScriptDraftInfo(
        String id,
        String projectId,
        String title,
        String strategy,
        String status,
        Integer totalScenes,
        String createdAt,
        String updatedAt) {

    /**
     * 从领域对象构建 DTO。
     *
     * @param draft 草稿领域对象
     */
    public static ScriptDraftInfo from(ScriptDraft draft) {
        return new ScriptDraftInfo(
                draft.id().toString(),
                draft.projectId().toString(),
                draft.title(),
                draft.strategy(),
                draft.status(),
                draft.totalScenes(),
                draft.createdAt().toString(),
                draft.updatedAt().toString());
    }
}
