package com.czx.wenshu.application.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.infrastructure.llm.UnconfiguredLlmClient;
import org.junit.jupiter.api.Test;

/** P5-01：验证 UnconfiguredLlmClient 降级行为与 PromptTemplate/JsonExtractor 工具（P5-02）。 */
class LlmClientDegradationTests {

    // ── P5-01 降级策略 ─────────────────────────────────────────────────────

    @Test
    void unconfiguredClientThrowsApiExceptionOnChat() {
        LlmClient client = new UnconfiguredLlmClient("test-model");
        assertThatThrownBy(() -> client.chat("system", "user"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("test-model");
    }

    @Test
    void unconfiguredClientMessageContainsHelpfulHint() {
        LlmClient client = new UnconfiguredLlmClient("创意模型 (Anthropic Claude)");
        assertThatThrownBy(() -> client.chat(null, "hello"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("API Key 未配置");
    }

    // ── P5-02 PromptTemplate ───────────────────────────────────────────────

    @Test
    void promptTemplateFillsAllVariables() {
        PromptTemplate tpl = new PromptTemplate("小说类型：{{genre}}，简介：{{synopsis}}");
        String result = tpl.fill(java.util.Map.of("genre", "玄幻", "synopsis", "一个热血少年的故事"));
        assertThat(result).isEqualTo("小说类型：玄幻，简介：一个热血少年的故事");
    }

    @Test
    void promptTemplateThrowsWhenPlaceholderUnfilled() {
        PromptTemplate tpl = new PromptTemplate("类型：{{genre}}，作者：{{author}}");
        assertThatThrownBy(() -> tpl.fill(java.util.Map.of("genre", "仙侠")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("author");
    }

    @Test
    void promptTemplatePlaceholdersReturnsAllNames() {
        PromptTemplate tpl = new PromptTemplate("{{a}} {{b}} {{a}} {{c}}");
        assertThat(tpl.placeholders()).containsExactly("a", "b", "c");
    }

    // ── P5-02 JsonExtractor ────────────────────────────────────────────────

    @Test
    void jsonExtractorExtractsObjectFromMarkdownBlock() {
        String llmOutput = """
                当然，以下是结果：
                ```json
                {"title": "天道至尊", "chapters": 10}
                ```
                希望对你有帮助。
                """;
        String json = JsonExtractor.extractFirstObject(llmOutput);
        assertThat(json).isNotNull();
        assertThat(json).contains("天道至尊");
    }

    @Test
    void jsonExtractorExtractsBareObject() {
        String llmOutput = "这是返回结果：{\"key\": \"value\", \"count\": 3}";
        String json = JsonExtractor.extractFirstObject(llmOutput);
        assertThat(json).isEqualTo("{\"key\": \"value\", \"count\": 3}");
    }

    @Test
    void jsonExtractorExtractsArray() {
        String llmOutput = "结果如下：[\"item1\", \"item2\", \"item3\"]";
        String json = JsonExtractor.extractFirstArray(llmOutput);
        assertThat(json).isEqualTo("[\"item1\", \"item2\", \"item3\"]");
    }

    @Test
    void jsonExtractorReturnsNullWhenNoJson() {
        assertThat(JsonExtractor.extractFirstObject("这里没有任何 JSON 内容")).isNull();
        assertThat(JsonExtractor.extractFirstArray("plain text only")).isNull();
    }

    @Test
    void jsonExtractorHandlesNestedJson() {
        String llmOutput = "{\"outer\": {\"inner\": [1, 2, 3]}, \"name\": \"test\"}";
        String json = JsonExtractor.extractFirstObject(llmOutput);
        assertThat(json).isEqualTo(llmOutput);
    }

    @Test
    void jsonExtractorParseObjectWithMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String llmOutput = "结果：{\"name\": \"张三\", \"age\": 25}";
        java.util.Map result = JsonExtractor.parseObject(llmOutput, java.util.Map.class, mapper);
        assertThat(result).isNotNull();
        assertThat(result.get("name")).isEqualTo("张三");
        assertThat(result.get("age")).isEqualTo(25);
    }
}
