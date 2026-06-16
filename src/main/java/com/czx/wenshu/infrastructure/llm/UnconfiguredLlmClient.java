package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;

/**
 * 未配置 API Key 时的降级实现：调用时返回业务异常，不影响应用启动。
 */
public class UnconfiguredLlmClient implements LlmClient {

    private final String modelDescription;

    public UnconfiguredLlmClient(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        throw new ApiException(ErrorCode.BAD_REQUEST,
                "AI 功能不可用：" + modelDescription + " 的 API Key 未配置，请在环境变量中设置对应 Key 后重启。");
    }
}
