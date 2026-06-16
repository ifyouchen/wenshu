package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.ChapterSummary;
import com.czx.wenshu.domain.project.ChapterSummaryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisChapterSummaryRepository implements ChapterSummaryRepository {

    private final ChapterSummaryMapper mapper;

    public MyBatisChapterSummaryRepository(ChapterSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ChapterSummary summary) {
        ChapterSummaryRecord record = toRecord(summary);
        if (mapper.findByChapterId(summary.chapterId().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
    }

    @Override
    public Optional<ChapterSummary> findByChapterId(UUID chapterId) {
        return Optional.ofNullable(mapper.findByChapterId(chapterId.toString())).map(this::toDomain);
    }

    @Override
    public void deleteByChapterId(UUID chapterId) {
        mapper.deleteByChapterId(chapterId.toString());
    }

    private ChapterSummary toDomain(ChapterSummaryRecord r) {
        return ChapterSummary.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getChapterId()),
                UUID.fromString(r.getProjectId()),
                r.getSummary(),
                r.getCreatedAt()
        );
    }

    private ChapterSummaryRecord toRecord(ChapterSummary s) {
        ChapterSummaryRecord r = new ChapterSummaryRecord();
        r.setId(s.id().toString());
        r.setChapterId(s.chapterId().toString());
        r.setProjectId(s.projectId().toString());
        r.setSummary(s.summary());
        r.setCreatedAt(s.createdAt());
        return r;
    }
}
