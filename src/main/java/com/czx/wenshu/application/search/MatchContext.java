package com.czx.wenshu.application.search;

/** 单条搜索命中的上下文片段（前缀、命中词、后缀各 ≤ 30 字符）。 */
public record MatchContext(String before, String match, String after) {
}
