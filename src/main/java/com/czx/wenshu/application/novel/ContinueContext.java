package com.czx.wenshu.application.novel;

/** 续写上下文（P5-07），供 SSE 控制器使用。 */
public record ContinueContext(String systemPrompt, String userPrompt) {
}
