package com.czx.wenshu.application.llm;

import java.util.function.Consumer;

/**
 * 流式 LLM 客户端接口（P5-07）。
 * 调用后立即返回，token 通过回调逐个推送，完成/错误也通过回调通知。
 */
public interface StreamingLlmClient {

    /**
     * 开始流式对话，立即返回（非阻塞）。
     *
     * @param systemPrompt 系统提示词（可为 null）
     * @param userPrompt   用户消息
     * @param onToken      每个新 token 的回调
     * @param onComplete   生成完成时的回调
     * @param onError      发生错误时的回调（含降级错误）
     */
    void streamChat(String systemPrompt, String userPrompt,
                    Consumer<String> onToken,
                    Runnable onComplete,
                    Consumer<Throwable> onError);
}
