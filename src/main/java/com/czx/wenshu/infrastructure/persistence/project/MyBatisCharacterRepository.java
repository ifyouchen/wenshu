package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.Character;
import com.czx.wenshu.domain.project.CharacterRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisCharacterRepository implements CharacterRepository {

    private final CharacterMapper mapper;

    public MyBatisCharacterRepository(CharacterMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Character save(Character c) {
        CharacterRecord record = toRecord(c);
        if (mapper.findById(c.id().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
        return c;
    }

    @Override
    public Optional<Character> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<Character> findByProjectId(UUID projectId) {
        return mapper.findByProjectId(projectId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        mapper.deleteById(id.toString());
    }

    @Override
    public boolean existsByIdAndProjectId(UUID id, UUID projectId) {
        return mapper.existsByIdAndProjectId(id.toString(), projectId.toString());
    }

    private Character toDomain(CharacterRecord r) {
        return Character.rehydrate(
                UUID.fromString(r.getId()), UUID.fromString(r.getProjectId()),
                r.getName(), r.getRole(), r.getAppearance(), r.getPersonality(),
                r.getAbilities(), r.getSpeechStyle(), r.getStatus(), r.isLocked(),
                r.getFirstChapterId() != null ? UUID.fromString(r.getFirstChapterId()) : null,
                r.getLastActiveChapterId() != null ? UUID.fromString(r.getLastActiveChapterId()) : null,
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private CharacterRecord toRecord(Character c) {
        CharacterRecord r = new CharacterRecord();
        r.setId(c.id().toString());
        r.setProjectId(c.projectId().toString());
        r.setName(c.name());
        r.setRole(c.role());
        r.setAppearance(c.appearance());
        r.setPersonality(c.personality());
        r.setAbilities(c.abilities());
        r.setSpeechStyle(c.speechStyle());
        r.setStatus(c.status());
        r.setLocked(c.locked());
        r.setFirstChapterId(c.firstChapterId() != null ? c.firstChapterId().toString() : null);
        r.setLastActiveChapterId(c.lastActiveChapterId() != null ? c.lastActiveChapterId().toString() : null);
        r.setCreatedAt(c.createdAt());
        r.setUpdatedAt(c.updatedAt());
        return r;
    }
}