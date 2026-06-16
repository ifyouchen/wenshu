package com.czx.wenshu.application.consistency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * LLM 返回的一致性问题 JSON 结构（P6-06），用于解析和入库。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsistencyItemJson(
        String type,
        String character,
        String chapterHint,
        String description,
        String suggestion) {
}
