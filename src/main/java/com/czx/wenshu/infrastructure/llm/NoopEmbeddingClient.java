package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.EmbeddingClient;

/**
 * 未配置嵌入模型时的降级实现（P6-04）。
 * 所有方法均安全返回 null/0/false，不影响应用启动。
 */
public class NoopEmbeddingClient implements EmbeddingClient {

    @Override
    public float[] embed(String text) {
        return null;
    }

    @Override
    public int dimension() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
