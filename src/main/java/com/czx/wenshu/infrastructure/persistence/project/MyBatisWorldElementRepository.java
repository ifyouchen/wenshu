package com.czx.wenshu.infrastructure.persistence.project;

import com.czx.wenshu.domain.project.WorldElement;
import com.czx.wenshu.domain.project.WorldElementRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisWorldElementRepository implements WorldElementRepository {

    private final WorldElementMapper mapper;

    public MyBatisWorldElementRepository(WorldElementMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WorldElement save(WorldElement element) {
        WorldElementRecord record = toRecord(element);
        if (mapper.findById(element.id().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
        return element;
    }

    @Override
    public Optional<WorldElement> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<WorldElement> findByProjectId(UUID projectId) {
        return mapper.findByProjectId(projectId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<WorldElement> findByProjectIdAndName(UUID projectId, String name) {
        return Optional.ofNullable(mapper.findByProjectIdAndName(projectId.toString(), name))
                .map(this::toDomain);
    }

    @Override
    public int countByProjectId(UUID projectId) {
        return mapper.countByProjectId(projectId.toString());
    }

    @Override
    public void deleteById(UUID id) {
        mapper.deleteById(id.toString());
    }

    @Override
    public boolean existsByIdAndProjectId(UUID id, UUID projectId) {
        return mapper.existsByIdAndProjectId(id.toString(), projectId.toString());
    }

    private WorldElement toDomain(WorldElementRecord r) {
        return WorldElement.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getProjectId()),
                r.getType(), r.getName(), r.getDescription(),
                r.getAliases() != null ? r.getAliases() : "[]",
                r.isLocked(), r.getCreatedAt());
    }

    private WorldElementRecord toRecord(WorldElement e) {
        WorldElementRecord r = new WorldElementRecord();
        r.setId(e.id().toString());
        r.setProjectId(e.projectId().toString());
        r.setType(e.type());
        r.setName(e.name());
        r.setDescription(e.description());
        r.setAliases(e.aliases() != null ? e.aliases() : "[]");
        r.setLocked(e.locked());
        r.setCreatedAt(e.createdAt());
        return r;
    }
}