package com.czx.wenshu.infrastructure.persistence.imports;

import com.czx.wenshu.domain.imports.ImportParseSession;
import com.czx.wenshu.domain.imports.ImportParseSessionRepository;
import com.czx.wenshu.domain.imports.ParsedChapterItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisImportParseSessionRepository implements ImportParseSessionRepository {

    private static final TypeReference<List<ParsedChapterItem>> CHAPTER_LIST_TYPE =
            new TypeReference<>() {};

    private final ImportParseSessionMapper mapper;
    private final ObjectMapper objectMapper;

    public MyBatisImportParseSessionRepository(ImportParseSessionMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ImportParseSession session) {
        ImportParseSessionRecord record = toRecord(session);
        if (mapper.findById(session.id().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
    }

    @Override
    public Optional<ImportParseSession> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        mapper.deleteById(id.toString());
    }

    private ImportParseSession toDomain(ImportParseSessionRecord r) {
        List<ParsedChapterItem> chapters;
        try {
            chapters = objectMapper.readValue(r.getParsedChapters(), CHAPTER_LIST_TYPE);
        } catch (Exception e) {
            chapters = List.of();
        }
        return ImportParseSession.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getProjectId()),
                UUID.fromString(r.getUserId()),
                chapters,
                r.getExpiresAt(),
                r.getCreatedAt()
        );
    }

    private ImportParseSessionRecord toRecord(ImportParseSession s) {
        ImportParseSessionRecord r = new ImportParseSessionRecord();
        r.setId(s.id().toString());
        r.setProjectId(s.projectId().toString());
        r.setUserId(s.userId().toString());
        try {
            r.setParsedChapters(objectMapper.writeValueAsString(s.chapters()));
        } catch (Exception e) {
            r.setParsedChapters("[]");
        }
        r.setExpiresAt(s.expiresAt());
        r.setCreatedAt(s.createdAt());
        return r;
    }
}
