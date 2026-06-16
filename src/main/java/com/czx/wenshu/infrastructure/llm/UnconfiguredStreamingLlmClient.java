package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.StreamingLlmClient;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import java.util.function.Consumer;

/** 未配置 API Key 时的流式降级实现，立即通过 onError 回调返回错误。 */
public class UnconfiguredStreamingLlmClient implements StreamingLlmClient {

    private final String modelDescription;

    public UnconfiguredStreamingLlmClient(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt,
                           Consumer<String> onToken, Runnable onComplete,
                           Consumer<Throwable> onError) {
        onError.accept(new ApiException(ErrorCode.BAD_REQUEST,
                "AI 功能不可用：" + modelDescription + " 的 API Key 未配置，请在环境变量中设置对应 Key 后重启。"));
    }
}
