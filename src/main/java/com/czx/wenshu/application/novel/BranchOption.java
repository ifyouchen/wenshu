package com.czx.wenshu.application.novel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 单条分支方向（P5-08）。 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BranchOption(int id, String direction, String summary) {
}
