package com.czx.wenshu.application.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板：从 classpath 加载模板文件，支持 {{varName}} 占位符替换。
 * 模板文件存放于 src/main/resources/prompts/ 目录。
 */
public class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");

    private final String template;

    public PromptTemplate(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
        this.template = template;
    }

    /**
     * 从 classpath 加载模板，路径相对于 classpath 根，例如 "prompts/skeleton.txt"。
     */
    public static PromptTemplate fromClasspath(String resourcePath) {
        try (InputStream is = PromptTemplate.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("找不到 prompt 模板资源：" + resourcePath);
            }
            return new PromptTemplate(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("读取 prompt 模板失败：" + resourcePath, e);
        }
    }

    /**
     * 填充占位符。所有 {{varName}} 都必须在 vars 中提供，否则抛出 IllegalStateException。
     */
    public String fill(Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        Matcher remaining = PLACEHOLDER.matcher(result);
        if (remaining.find()) {
            throw new IllegalStateException("Prompt 模板存在未填充的占位符：{{" + remaining.group(1) + "}}");
        }
        return result;
    }

    /** 列出模板中所有占位符名称（调试或预校验用）。 */
    public java.util.Set<String> placeholders() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(template);
        while (m.find()) names.add(m.group(1));
        return names;
    }

    public String raw() {
        return template;
    }
}
