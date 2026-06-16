package com.czx.wenshu.application.llm;

/**
 * 文本向量嵌入客户端接口（P6-04）。
 * 无嵌入模型时降级返回 null，应用正常启动，pgvector 检索退化为文本搜索。
 */
public interface EmbeddingClient {

    /**
     * 生成文本的向量嵌入。
     * 降级实现返回 null，调用方需判空。
     *
     * @param text 待嵌入的文本
     * @return 向量数组（float[]），不可用时返回 null
     */
    float[] embed(String text);

    /**
     * 返回向量维度，不可用时返回 0。
     */
    int dimension();

    /**
     * 是否可用（API Key 已配置且模型已初始化）。
     */
    boolean isAvailable();
}
