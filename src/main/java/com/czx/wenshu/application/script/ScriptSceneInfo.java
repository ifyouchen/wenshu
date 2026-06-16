package com.czx.wenshu.application.script;

import com.czx.wenshu.domain.script.ScriptScene;

/**
 * 剧本场景响应 DTO（P7-01）。
 * 包含场景内容和乐观锁版本号，前端编辑时需回传 version。
 */
public record ScriptSceneInfo(
        String id,
        String draftId,
        int sceneIndex,
        String location,
        String timeDesc,
        String content,
        String sourceContent,
        int version,
        String updatedAt) {

    /**
     * 从领域对象构建 DTO。
     *
     * @param scene 场景领域对象
     */
    public static ScriptSceneInfo from(ScriptScene scene) {
        return new ScriptSceneInfo(
                scene.id().toString(),
                scene.draftId().toString(),
                scene.sceneIndex(),
                scene.location(),
                scene.timeDesc(),
                scene.content(),
                scene.sourceContent(),
                scene.version(),
                scene.updatedAt().toString());
    }
}
