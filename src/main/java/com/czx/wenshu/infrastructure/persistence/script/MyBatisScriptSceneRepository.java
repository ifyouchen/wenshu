package com.czx.wenshu.infrastructure.persistence.script;

import com.czx.wenshu.domain.script.ScriptScene;
import com.czx.wenshu.domain.script.ScriptSceneRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的剧本场景仓储（P7-01）。 */
@Repository
public class MyBatisScriptSceneRepository implements ScriptSceneRepository {

    private final ScriptSceneMapper mapper;

    public MyBatisScriptSceneRepository(ScriptSceneMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ScriptScene scene) {
        ScriptSceneRecord r = toRecord(scene);
        if (mapper.findById(scene.id().toString()) == null) {
            mapper.insert(r);
        } else {
            mapper.update(r);
        }
    }

    @Override
    public Optional<ScriptScene> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<ScriptScene> findByDraftId(UUID draftId, int offset, int limit) {
        return mapper.findByDraftId(draftId.toString(), offset, limit)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public int countByDraftId(UUID draftId) {
        return mapper.countByDraftId(draftId.toString());
    }

    private ScriptScene toDomain(ScriptSceneRecord r) {
        return ScriptScene.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getDraftId()),
                r.getEpisodeId() != null ? UUID.fromString(r.getEpisodeId()) : null,
                r.getSceneIndex(), r.getLocation(), r.getTimeDesc(), r.getInterior(),
                r.getCharacters(), r.getContent(), r.getSourceContent(),
                r.getVersion(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private ScriptSceneRecord toRecord(ScriptScene s) {
        ScriptSceneRecord r = new ScriptSceneRecord();
        r.setId(s.id().toString());
        r.setDraftId(s.draftId().toString());
        r.setEpisodeId(s.episodeId() != null ? s.episodeId().toString() : null);
        r.setSceneIndex(s.sceneIndex());
        r.setLocation(s.location());
        r.setTimeDesc(s.timeDesc());
        r.setInterior(s.interior());
        r.setCharacters(s.characters());
        r.setContent(s.content());
        r.setSourceContent(s.sourceContent());
        r.setVersion(s.version());
        r.setCreatedAt(s.createdAt());
        r.setUpdatedAt(s.updatedAt());
        return r;
    }
}
