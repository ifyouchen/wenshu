package com.czx.wenshu.infrastructure.config;

import com.czx.wenshu.application.llm.EmbeddingClient;
import com.czx.wenshu.infrastructure.llm.LangChain4jEmbeddingClient;
import com.czx.wenshu.infrastructure.llm.NoopEmbeddingClient;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量嵌入客户端 Spring 配置（P6-04）。
 * 无 API Key 时注册 NoopEmbeddingClient，应用正常启动，检索退化为文本搜索。
 */
@Configuration
public class EmbeddingConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);
    /** 嵌入向量维度，与 character_event_embeddings 表的 VECTOR(1024) 一致。 */
    private static final int EMBEDDING_DIMENSION = 1024;

    /**
     * 创建嵌入客户端 Bean。
     * 若 DeepSeek API Key 已配置，则使用 DeepSeek 的 OpenAI 兼容嵌入接口；
     * 否则降级为 NoopEmbeddingClient。
     *
     * @param props 项目配置属性
     */
    @Bean
    public EmbeddingClient embeddingClient(WenshuProperties props) {
        WenshuProperties.Llm llm = props.getLlm();
        if (llm == null || llm.getDeepseekApiKey() == null || llm.getDeepseekApiKey().isBlank()) {
            log.warn("[EmbeddingConfig] DEEPSEEK_API_KEY 未配置，向量嵌入降级为 NoopEmbeddingClient，pgvector 检索不可用。");
            return new NoopEmbeddingClient();
        }
        try {
            OpenAiEmbeddingModel model = OpenAiEmbeddingModel.builder()
                    .apiKey(llm.getDeepseekApiKey())
                    .baseUrl("https://api.deepseek.com/v1")
                    .modelName("embedding-2")
                    .build();
            log.info("[EmbeddingConfig] 向量嵌入模型已就绪（DeepSeek embedding-2）");
            return new LangChain4jEmbeddingClient(model, EMBEDDING_DIMENSION);
        } catch (Exception e) {
            log.warn("[EmbeddingConfig] 嵌入模型初始化失败，降级为 Noop", e);
            return new NoopEmbeddingClient();
        }
    }
}
