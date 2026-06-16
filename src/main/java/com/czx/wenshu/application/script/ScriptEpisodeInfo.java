package com.czx.wenshu.application.script;

import com.czx.wenshu.domain.script.ScriptEpisode;

/**
 * 剧本集数响应 DTO（P7-07）。
 */
public record ScriptEpisodeInfo(
        String id,
        String draftId,
        int episodeNo,
        String title,
        int sortOrder,
        String createdAt) {

    /**
     * 从领域对象构建 DTO。
     *
     * @param episode 集数领域对象
     */
    public static ScriptEpisodeInfo from(ScriptEpisode episode) {
        return new ScriptEpisodeInfo(
                episode.id().toString(),
                episode.draftId().toString(),
                episode.episodeNo(),
                episode.title(),
                episode.sortOrder(),
                episode.createdAt().toString());
    }
}
