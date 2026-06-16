package com.czx.wenshu.application.project;

import com.czx.wenshu.application.llm.JsonExtractor;
import com.czx.wenshu.domain.project.ChapterKeyEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * 章节关键事件响应 DTO（P6-03）。
 */
public record KeyEventInfo(
        String id,
        String chapterId,
        String projectId,
        String eventText,
        String eventType,
        List<String> characters,
        double importance,
        String createdAt) {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    /**
     * 从领域对象构建 DTO，并解析角色 JSON 字符串为列表。
     *
     * @param event  关键事件领域对象
     * @param mapper Jackson ObjectMapper
     */
    public static KeyEventInfo from(ChapterKeyEvent event, ObjectMapper mapper) {
        List<String> charList = parseCharacters(event.characters(), mapper);
        return new KeyEventInfo(
                event.id().toString(),
                event.chapterId().toString(),
                event.projectId().toString(),
                event.eventText(),
                event.eventType(),
                charList,
                event.importance(),
                event.createdAt().toString()
        );
    }

    private static List<String> parseCharacters(String json, ObjectMapper mapper) {
        if (json == null || "[]".equals(json.strip())) return List.of();
        try {
            return mapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }
}
