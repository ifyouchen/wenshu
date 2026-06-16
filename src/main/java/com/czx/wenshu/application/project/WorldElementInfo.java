package com.czx.wenshu.application.project;

import com.czx.wenshu.domain.project.WorldElement;
import java.util.List;

public record WorldElementInfo(String id, String projectId, String type, String name,
                                String description, List<String> aliases, boolean locked, String createdAt) {

    public static WorldElementInfo from(WorldElement e) {
        return new WorldElementInfo(e.id().toString(), e.projectId().toString(), e.type(), e.name(),
                e.description(), parseAliases(e.aliases()), e.locked(), e.createdAt().toString());
    }

    private static List<String> parseAliases(String json) {
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
        // Simple parser for JSON array of plain strings (no nested quotes or complex escapes needed for typical aliases)
        List<String> result = new java.util.ArrayList<>();
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
                while (i < inner.length() && (inner.charAt(i) == ',' || inner.charAt(i) == ' ')) {
                    i++;
                }
            } else {
                i++;
            }
        }
        return result;
    }
}