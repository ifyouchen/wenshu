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
import java.util.function.Consumer;

/** Anthropic Claude 流式模型适配，用于 SSE 续写（P5-07）。 */
public class LangChain4jAnthropicStreamingLlmClient implements StreamingLlmClient {

    private final StreamingChatLanguageModel model;

    public LangChain4jAnthropicStreamingLlmClient(StreamingChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt,
                           Consumer<String> onToken, Runnable onComplete,
                           Consumer<Throwable> onError) {
        List<ChatMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.add(UserMessage.from(userPrompt));

        model.generate(messages, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                onToken.accept(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }
}
