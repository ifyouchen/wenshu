package com.czx.wenshu.infrastructure.config;

import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.application.llm.StreamingLlmClient;
import com.czx.wenshu.infrastructure.llm.LangChain4jAnthropicLlmClient;
import com.czx.wenshu.infrastructure.llm.LangChain4jAnthropicStreamingLlmClient;
import com.czx.wenshu.infrastructure.llm.LangChain4jDeepSeekLlmClient;
import com.czx.wenshu.infrastructure.llm.UnconfiguredLlmClient;
import com.czx.wenshu.infrastructure.llm.UnconfiguredStreamingLlmClient;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 客户端 Spring 配置。
 * 无 API Key 时注册 UnconfiguredLlmClient，应用正常启动，调用时返回业务异常。
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    /**
     * 创意模型（Claude）— 用于长文生成、续写、改编等高质量场景。
     * qualifier: "creativeLlmClient"
     */
    @Bean
    @Qualifier("creativeLlmClient")
    public LlmClient creativeLlmClient(WenshuProperties props) {
        WenshuProperties.Llm llm = props.getLlm();
        if (llm == null || llm.getAnthropicApiKey() == null || llm.getAnthropicApiKey().isBlank()) {
            log.warn("ANTHROPIC_API_KEY 未配置，创意模型降级为 UnconfiguredLlmClient，AI 写作功能不可用。");
            return new UnconfiguredLlmClient("创意模型 (Anthropic Claude)");
        }
        AnthropicChatModel model = AnthropicChatModel.builder()
                .apiKey(llm.getAnthropicApiKey())
                .modelName(llm.getCreativeModel() != null ? llm.getCreativeModel() : "claude-sonnet-4-6")
                .maxTokens(4096)
                .build();
        log.info("创意模型已就绪：{}", llm.getCreativeModel());
        return new LangChain4jAnthropicLlmClient(model);
    }

    /**
     * 工具模型（DeepSeek）— 用于格式化提取、批注、章节摘要等低延迟场景。
     * qualifier: "utilityLlmClient"
     */
    @Bean
    @Qualifier("utilityLlmClient")
    public LlmClient utilityLlmClient(WenshuProperties props) {
        WenshuProperties.Llm llm = props.getLlm();
        if (llm == null || llm.getDeepseekApiKey() == null || llm.getDeepseekApiKey().isBlank()) {
            log.warn("DEEPSEEK_API_KEY 未配置，工具模型降级为 UnconfiguredLlmClient，AI 工具功能不可用。");
            return new UnconfiguredLlmClient("工具模型 (DeepSeek)");
        }
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(llm.getDeepseekApiKey())
                .baseUrl("https://api.deepseek.com/v1")
                .modelName(llm.getUtilityModel() != null ? llm.getUtilityModel() : "deepseek-chat")
                .maxTokens(4096)
                .build();
        log.info("工具模型已就绪：{}", llm.getUtilityModel());
        return new LangChain4jDeepSeekLlmClient(model);
    }

    /**
     * 创意流式模型（Claude Streaming）— 用于 SSE 续写（P5-07）。
     * qualifier: "streamingCreativeLlmClient"
     */
    @Bean
    @Qualifier("streamingCreativeLlmClient")
    public StreamingLlmClient streamingCreativeLlmClient(WenshuProperties props) {
        WenshuProperties.Llm llm = props.getLlm();
        if (llm == null || llm.getAnthropicApiKey() == null || llm.getAnthropicApiKey().isBlank()) {
            log.warn("ANTHROPIC_API_KEY 未配置，流式创意模型降级为 UnconfiguredStreamingLlmClient。");
            return new UnconfiguredStreamingLlmClient("流式创意模型 (Anthropic Claude)");
        }
        AnthropicStreamingChatModel model = AnthropicStreamingChatModel.builder()
                .apiKey(llm.getAnthropicApiKey())
                .modelName(llm.getCreativeModel() != null ? llm.getCreativeModel() : "claude-sonnet-4-6")
                .maxTokens(4096)
                .build();
        log.info("流式创意模型已就绪：{}", llm.getCreativeModel());
        return new LangChain4jAnthropicStreamingLlmClient(model);
    }
}
