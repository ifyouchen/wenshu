package com.czx.wenshu.domain.project;

import java.util.List;
import java.util.UUID;

/**
 * 章节关键事件仓储端口（P6-03）。
 */
public interface ChapterKeyEventRepository {

    /**
     * 保存一条关键事件。若同一章节同一事件文本已存在，则忽略（依赖 UNIQUE 约束）。
     *
     * @param event 关键事件实体
     */
    void save(ChapterKeyEvent event);

    /**
     * 按章节 ID 查询所有关键事件，按重要程度降序排列。
     *
     * @param chapterId 章节 ID
     * @return 关键事件列表
     */
    List<ChapterKeyEvent> findByChapterId(UUID chapterId);

    /**
     * 按作品 ID 查询所有关键事件，按重要程度降序排列。
     *
     * @param projectId 作品 ID
     * @return 关键事件列表
     */
    List<ChapterKeyEvent> findByProjectId(UUID projectId);

    /**
     * 删除指定章节的所有关键事件（章节重新提取时使用）。
     *
     * @param chapterId 章节 ID
     */
    void deleteByChapterId(UUID chapterId);
}
