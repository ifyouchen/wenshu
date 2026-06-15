package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.ChapterSnapshot;
import com.czx.wenshu.domain.project.ChapterSnapshotRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisChapterSnapshotRepository implements ChapterSnapshotRepository {

    private final ChapterSnapshotMapper mapper;

    public MyBatisChapterSnapshotRepository(ChapterSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ChapterSnapshot save(ChapterSnapshot snapshot) {
        ChapterSnapshotRecord record = toRecord(snapshot);
        mapper.insert(record);
        return snapshot;
    }

    @Override
    public List<ChapterSnapshot> findByChapterId(UUID chapterId) {
        return mapper.findByChapterId(chapterId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ChapterSnapshot> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        throw new UnsupportedOperationException("Not needed yet");
    }

    private ChapterSnapshot toDomain(ChapterSnapshotRecord r) {
        return ChapterSnapshot.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getChapterId()),
                r.getContent(), r.getWordCount(), r.getSnapshotType(), r.getLabel(), r.getCreatedAt());
    }

    private ChapterSnapshotRecord toRecord(ChapterSnapshot s) {
        ChapterSnapshotRecord r = new ChapterSnapshotRecord();
        r.setId(s.id().toString());
        r.setChapterId(s.chapterId().toString());
        r.setContent(s.content());
        r.setWordCount(s.wordCount());
        r.setSnapshotType(s.snapshotType());
        r.setLabel(s.label());
        r.setCreatedAt(s.createdAt());
        return r;
    }
}