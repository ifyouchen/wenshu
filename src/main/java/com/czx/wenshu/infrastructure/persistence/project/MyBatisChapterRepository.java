package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.Chapter;
import com.czx.wenshu.domain.project.ChapterRepository;
import com.czx.wenshu.domain.project.ChapterStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisChapterRepository implements ChapterRepository {

    private final ChapterMapper chapterMapper;

    public MyBatisChapterRepository(ChapterMapper chapterMapper) {
        this.chapterMapper = chapterMapper;
    }

    @Override
    public Chapter save(Chapter chapter) {
        ChapterRecord record = toRecord(chapter);
        if (chapterMapper.findById(chapter.id().toString()) == null) {
            chapterMapper.insert(record);
        } else {
            chapterMapper.update(record);
        }
        return chapter;
    }

    @Override
    public Optional<Chapter> findById(UUID id) {
        return Optional.ofNullable(chapterMapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<Chapter> findByVolumeId(UUID volumeId) {
        return chapterMapper.findByVolumeId(volumeId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        chapterMapper.deleteById(id.toString());
    }

    @Override
    public boolean existsByIdAndProjectId(UUID id, UUID projectId) {
        return chapterMapper.existsByIdAndProjectId(id.toString(), projectId.toString());
    }

    @Override
    public List<Chapter> findByProjectId(UUID projectId) {
        return chapterMapper.findByProjectId(projectId.toString()).stream().map(this::toDomain).toList();
    }

    private Chapter toDomain(ChapterRecord r) {
        return Chapter.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getVolumeId()),
                UUID.fromString(r.getProjectId()), r.getTitle(), r.getOutline(), r.getContent(),
                r.getWordCount(), r.getSortOrder(), ChapterStatus.fromValue(r.getStatus()),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private ChapterRecord toRecord(Chapter c) {
        ChapterRecord r = new ChapterRecord();
        r.setId(c.id().toString());
        r.setVolumeId(c.volumeId().toString());
        r.setProjectId(c.projectId().toString());
        r.setTitle(c.title());
        r.setOutline(c.outline());
        r.setContent(c.content());
        r.setWordCount(c.wordCount());
        r.setSortOrder(c.sortOrder());
        r.setStatus(c.status().value());
        r.setCreatedAt(c.createdAt());
        r.setUpdatedAt(c.updatedAt());
        return r;
    }
}