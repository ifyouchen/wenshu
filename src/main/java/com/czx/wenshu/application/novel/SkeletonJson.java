package com.czx.wenshu.application.novel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** LLM 生成的骨架 JSON 结构（P5-04/P5-05）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SkeletonJson(
        String title,
        String theme,
        List<VolumeNode> volumes,
        List<CharacterNode> characters) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VolumeNode(String title, String conflict, List<ChapterNode> chapters) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterNode(String title, String outline) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CharacterNode(String name, String role, String description) {}
}
