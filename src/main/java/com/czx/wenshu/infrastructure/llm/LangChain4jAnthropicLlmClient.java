package com.czx.wenshu.infrastructure.llm;

import com.czx.wenshu.application.llm.LlmClient;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.List;

/** Anthropic Claude 模型的 LangChain4j 适配，用于创意写作（creative）场景。 */
public class LangChain4jAnthropicLlmClient implements LlmClient {

    private final ChatLanguageModel model;

    public LangChain4jAnthropicLlmClient(ChatLanguageModel model) {
        this.model = model;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            List<dev.langchain4j.data.message.ChatMessage> messages;
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages = List.of(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt));
            } else {
                messages = List.of(UserMessage.from(userPrompt));
            }
            Response<AiMessage> response = model.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Anthropic LLM 调用失败：" + e.getMessage());
        }
    }
}
