package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.StreamingLlmClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Anthropic Claude 流式模型适配，用于 SSE 续写（P5-07）。 */
public class LangChain4jAnthropicStreamingLlmClient implements StreamingLlmClient {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jAnthropicStreamingLlmClient.class);

    private final StreamingChatLanguageModel model;

    public LangChain4jAnthropicStreamingLlmClient(StreamingChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt,
                           Consumer<String> onToken, Runnable onComplete,
                           Consumer<Throwable> onError) {
        long start = System.currentTimeMillis();
        AtomicInteger tokenCount = new AtomicInteger(0);
        log.info("[StreamingClient] 流式调用开始 输入字符={}", (systemPrompt != null ? systemPrompt.length() : 0) + userPrompt.length());

        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userPrompt));

        model.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                tokenCount.incrementAndGet();
                onToken.accept(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                long elapsed = System.currentTimeMillis() - start;
                log.info("[StreamingClient] 流式调用完成 token数={} 耗时={}ms", tokenCount.get(), elapsed);
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                long elapsed = System.currentTimeMillis() - start;
                log.warn("[StreamingClient] 流式调用失败 耗时={}ms error={}", elapsed, error.getMessage());
                onError.accept(error);
            }
        });
    }
}
