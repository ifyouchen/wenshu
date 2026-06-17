package com.czx.wenshu.application.user;

import com.czx.wenshu.domain.user.StyleTemplate;
import java.util.ArrayList;
import java.util.List;

public record StyleTemplateInfo(String id, String userId, String name, String templateType,
                                List<String> genres, String prompt, boolean active,
                                String createdAt, String updatedAt) {

    public static StyleTemplateInfo from(StyleTemplate template) {
        return new StyleTemplateInfo(template.id().toString(), template.userId().toString(),
                template.name(), template.templateType(), parseJsonArray(template.genres()),
                template.prompt(), template.active(), template.createdAt().toString(),
                template.updatedAt().toString());
    }

    static List<String> parseJsonArray(String json) {
        if (json == null || "[]".equals(json.strip())) {
            return List.of();
        }
        String trimmed = json.strip();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).strip();
        if (inner.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < inner.length()) {
            if (inner.charAt(i) == '"') {
                int end = i + 1;
                while (end < inner.length()) {
                    if (inner.charAt(end) == '\\') {
                        end += 2;
                    } else if (inner.charAt(end) == '"') {
                        break;
                    } else {
                        end++;
                    }
                }
                if (end < inner.length()) {
                    result.add(inner.substring(i + 1, end)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\"));
                }
                i = end + 1;
            } else {
                i++;
            }
        }
        return result;
    }
}
