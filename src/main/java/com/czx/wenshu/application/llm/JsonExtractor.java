package com.czx.wenshu.application.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 LLM 输出中健壮地提取 JSON。
 * 处理场景：markdown 代码块（```json...```）、纯 JSON、JSON 前后有描述文本。
 */
public final class JsonExtractor {

    private static final Pattern CODE_BLOCK_JSON = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_OBJECT = Pattern.compile("\\{[\\s\\S]*}", Pattern.DOTALL);
    private static final Pattern FIRST_ARRAY = Pattern.compile("\\[[\\s\\S]*]", Pattern.DOTALL);

    private JsonExtractor() {}

    /**
     * 从文本中提取第一个 JSON 对象字符串（{...}）。
     * 先尝试提取代码块内的 JSON，再从全文扫描第一个 `{`。
     * @return JSON 字符串，若未找到则返回 null
     */
    public static String extractFirstObject(String text) {
        if (text == null || text.isBlank()) return null;
        // 1. 尝试 markdown 代码块
        Matcher codeMatcher = CODE_BLOCK_JSON.matcher(text);
        while (codeMatcher.find()) {
            String candidate = codeMatcher.group(1).strip();
            if (candidate.startsWith("{")) {
                String balanced = extractBalanced(candidate, '{', '}');
                if (balanced != null) return balanced;
            }
        }
        // 2. 直接扫描第一个 {
        int start = text.indexOf('{');
        if (start < 0) return null;
        return extractBalanced(text.substring(start), '{', '}');
    }

    /**
     * 从文本中提取第一个 JSON 数组字符串（[...]）。
     * @return JSON 字符串，若未找到则返回 null
     */
    public static String extractFirstArray(String text) {
        if (text == null || text.isBlank()) return null;
        // 1. 尝试 markdown 代码块
        Matcher codeMatcher = CODE_BLOCK_JSON.matcher(text);
        while (codeMatcher.find()) {
            String candidate = codeMatcher.group(1).strip();
            if (candidate.startsWith("[")) {
                String balanced = extractBalanced(candidate, '[', ']');
                if (balanced != null) return balanced;
            }
        }
        // 2. 直接扫描第一个 [
        int start = text.indexOf('[');
        if (start < 0) return null;
        return extractBalanced(text.substring(start), '[', ']');
    }

    /**
     * 从 LLM 输出中提取 JSON 对象，并反序列化为指定类型。
     * @return 反序列化结果，若 JSON 提取或解析失败则返回 null（容错）
     */
    public static <T> T parseObject(String text, Class<T> type, ObjectMapper mapper) {
        String json = extractFirstObject(text);
        if (json == null) return null;
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 LLM 输出中提取 JSON 数组，并反序列化为指定类型。
     * @return 反序列化结果，若 JSON 提取或解析失败则返回 null（容错）
     */
    public static <T> T parseArray(String text, TypeReference<T> typeRef, ObjectMapper mapper) {
        String json = extractFirstArray(text);
        if (json == null) return null;
        try {
            return mapper.readValue(json, typeRef);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从文本开头提取配对括号的完整子串（处理嵌套，但忽略字符串中的括号）。
     */
    private static String extractBalanced(String text, char open, char close) {
        if (text.isEmpty() || text.charAt(0) != open) return null;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return text.substring(0, i + 1);
                }
            }
        }
        return null;
    }
}
