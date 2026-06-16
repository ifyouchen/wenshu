package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.ChapterKeyEvent;
import com.czx.wenshu.domain.project.ChapterKeyEventRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的章节关键事件仓储（P6-03）。 */
@Repository
public class MyBatisChapterKeyEventRepository implements ChapterKeyEventRepository {

    /** MyBatis Mapper。 */
    private final ChapterKeyEventMapper mapper;

    public MyBatisChapterKeyEventRepository(ChapterKeyEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ChapterKeyEvent event) {
        mapper.insertIgnoreDuplicate(toRecord(event));
    }

    @Override
    public List<ChapterKeyEvent> findByChapterId(UUID chapterId) {
        return mapper.findByChapterId(chapterId.toString()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<ChapterKeyEvent> findByProjectId(UUID projectId) {
        return mapper.findByProjectId(projectId.toString()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteByChapterId(UUID chapterId) {
        mapper.deleteByChapterId(chapterId.toString());
    }

    private ChapterKeyEvent toDomain(ChapterKeyEventRecord r) {
        return ChapterKeyEvent.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getProjectId()),
                UUID.fromString(r.getChapterId()),
                r.getEventText(),
                r.getEventType(),
                r.getCharacters(),
                r.getImportance(),
                r.getCreatedAt()
        );
    }

    private ChapterKeyEventRecord toRecord(ChapterKeyEvent e) {
        ChapterKeyEventRecord r = new ChapterKeyEventRecord();
        r.setId(e.id().toString());
        r.setProjectId(e.projectId().toString());
        r.setChapterId(e.chapterId().toString());
        r.setEventText(e.eventText());
        r.setEventType(e.eventType());
        r.setCharacters(e.characters());
        r.setImportance(e.importance());
        r.setCreatedAt(e.createdAt());
        return r;
    }
}
