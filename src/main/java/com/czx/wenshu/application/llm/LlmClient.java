package com.czx.wenshu.application.llm;

/**
 * LLM 调用抽象接口。
 * 当 API Key 未配置时实现类应抛出 ApiException(BAD_REQUEST)，而不是在应用启动时失败。
 */
public interface LlmClient {

    /**
     * 发送 system + user 消息对，返回 assistant 文本回复。
     *
     * @param systemPrompt 系统提示词（可为 null）
     * @param userPrompt   用户消息
     * @return LLM 生成的文本
     */
    String chat(String systemPrompt, String userPrompt);
}
