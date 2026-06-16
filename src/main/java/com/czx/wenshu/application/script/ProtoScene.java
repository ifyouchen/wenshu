package com.czx.wenshu.application.script;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * LLM 场景分割结果的 JSON 结构（P7-02）。
 * 包含原文片段和场景元数据，用于后续剧本转换。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProtoScene(
        String location,
        String timeDesc,
        Boolean isInterior,
        List<String> characters,
        String sourceContent) {
}
