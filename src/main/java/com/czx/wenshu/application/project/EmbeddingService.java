package com.czx.wenshu.application.project;

import com.czx.wenshu.application.llm.EmbeddingClient;
import com.czx.wenshu.domain.project.ChapterKeyEvent;
import com.czx.wenshu.domain.project.ChapterKeyEventRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 向量嵌入服务（P6-04）。
 * 在关键事件提取完成后，将事件文本写入 pgvector 表；
 * 提供基于 pgvector 余弦相似度的语义检索（降级时退化为关键词匹配）。
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    /** 向量嵌入客户端。 */
    private final EmbeddingClient embeddingClient;
    /** 关键事件仓储，用于关键词回退检索。 */
    private final ChapterKeyEventRepository keyEventRepository;
    /** JDBC 模板，用于执行 pgvector 相关 SQL。 */
    private final JdbcTemplate jdbcTemplate;

    public EmbeddingService(EmbeddingClient embeddingClient,
                             ChapterKeyEventRepository keyEventRepository,
                             JdbcTemplate jdbcTemplate) {
        this.embeddingClient = embeddingClient;
        this.keyEventRepository = keyEventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 将章节关键事件列表写入向量嵌入表（P6-04）。
     * 若嵌入客户端不可用，则跳过写入并记录警告。
     *
     * @param chapterId     章节 ID
     * @param characterId   关联角色 ID（可为 null）
     * @param projectId     作品 ID
     * @param keyEvents     已提取的关键事件列表
     */
    @Transactional
    public void saveKeyEventEmbeddings(UUID chapterId, UUID characterId,
                                        UUID projectId, List<ChapterKeyEvent> keyEvents) {
        if (!embeddingClient.isAvailable()) {
            log.warn("[EmbeddingService] 嵌入客户端不可用，跳过写入 chapterId={}", chapterId);
            return;
        }
        if (keyEvents == null || keyEvents.isEmpty()) {
            return;
        }
        int savedCount = 0;
        for (ChapterKeyEvent event : keyEvents) {
            try {
                float[] vector = embeddingClient.embed(event.eventText());
                if (vector == null) continue;
                String vectorStr = toVectorString(vector);
                jdbcTemplate.update(
                        "INSERT INTO character_event_embeddings (character_id, project_id, event_text, chapter_id, embedding) " +
                        "VALUES (?, ?, ?, ?, ?::vector) " +
                        "ON CONFLICT DO NOTHING",
                        characterId != null ? characterId.toString() : null,
                        projectId.toString(),
                        event.eventText(),
                        chapterId.toString(),
                        vectorStr
                );
                savedCount++;
            } catch (Exception e) {
                log.warn("[EmbeddingService] 写入向量失败 eventText={}", event.eventText(), e);
            }
        }
        log.info("[EmbeddingService] 向量写入完成 chapterId={} 成功数={}", chapterId, savedCount);
    }

    /**
     * 基于语义相似度检索相关关键事件（P6-04）。
     * 嵌入客户端不可用时，退化为关键词文本匹配。
     *
     * @param projectId 作品 ID
     * @param query     查询文本
     * @param topK      返回最大数量
     * @return 关键事件信息列表（按相似度降序）
     */
    @Transactional(readOnly = true)
    public List<ChapterKeyEvent> searchSimilar(UUID projectId, String query, int topK) {
        if (!embeddingClient.isAvailable()) {
            log.debug("[EmbeddingService] 嵌入不可用，退化为关键词检索 projectId={} query={}", projectId, query);
            return keyEventRepository.findByProjectId(projectId).stream()
                    .filter(e -> query != null && e.eventText().contains(query))
                    .limit(topK)
                    .toList();
        }
        try {
            float[] queryVector = embeddingClient.embed(query);
            if (queryVector == null) {
                return List.of();
            }
            String vectorStr = toVectorString(queryVector);
            List<String> eventTexts = jdbcTemplate.queryForList(
                    "SELECT DISTINCT event_text FROM character_event_embeddings " +
                    "WHERE project_id = ?::uuid " +
                    "ORDER BY embedding <=> ?::vector LIMIT ?",
                    String.class,
                    projectId.toString(), vectorStr, topK
            );
            log.debug("[EmbeddingService] pgvector 检索完成 projectId={} 命中数={}", projectId, eventTexts.size());
            return keyEventRepository.findByProjectId(projectId).stream()
                    .filter(e -> eventTexts.contains(e.eventText()))
                    .toList();
        } catch (Exception e) {
            log.warn("[EmbeddingService] pgvector 检索失败，退化为关键词检索", e);
            return keyEventRepository.findByProjectId(projectId).stream()
                    .filter(evt -> query != null && evt.eventText().contains(query))
                    .limit(topK)
                    .toList();
        }
    }

    /**
     * 将 float[] 向量转换为 pgvector 格式字符串，例如 [0.1,0.2,0.3]。
     *
     * @param vector 向量数组
     */
    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
