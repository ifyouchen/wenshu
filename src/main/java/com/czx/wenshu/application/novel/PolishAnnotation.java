package com.czx.wenshu.application.novel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 基础校正的单条建议（P5-09）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PolishAnnotation(String original, String suggested, String reason) {
}
