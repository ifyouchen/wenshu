package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.EmbeddingClient;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 LangChain4j EmbeddingModel 的向量嵌入客户端实现（P6-04）。
 * 封装模型调用并对外统一为 float[] 接口，失败时降级返回 null。
 */
public class LangChain4jEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jEmbeddingClient.class);

    /** LangChain4j 嵌入模型实例。 */
    private final EmbeddingModel embeddingModel;
    /** 向量维度。 */
    private final int dimension;

    public LangChain4jEmbeddingClient(EmbeddingModel embeddingModel, int dimension) {
        this.embeddingModel = embeddingModel;
        this.dimension = dimension;
    }

    /**
     * 调用嵌入模型生成向量，失败时返回 null（不抛异常，保证降级安全）。
     *
     * @param text 待嵌入文本
     * @return 向量数组，失败时返回 null
     */
    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            Response<Embedding> response = embeddingModel.embed(text);
            float[] vector = response.content().vector();
            log.debug("[EmbeddingClient] 向量生成完成 长度={}", vector.length);
            return vector;
        } catch (Exception e) {
            log.warn("[EmbeddingClient] 向量生成失败 error={}", e.getMessage());
            return null;
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
