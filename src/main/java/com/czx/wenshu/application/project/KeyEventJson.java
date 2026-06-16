package com.czx.wenshu.application.project;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * LLM 返回的关键事件 JSON 结构（P6-03），用于解析和入库。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeyEventJson(
        String eventText,
        String eventType,
        List<String> characters,
        Double importance) {
}
