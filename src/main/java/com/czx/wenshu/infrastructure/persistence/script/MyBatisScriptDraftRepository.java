package com.czx.wenshu.infrastructure.persistence.script;

import com.czx.wenshu.domain.script.ScriptDraft;
import com.czx.wenshu.domain.script.ScriptDraftRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的剧本草稿仓储（P7-01）。 */
@Repository
public class MyBatisScriptDraftRepository implements ScriptDraftRepository {

    private final ScriptDraftMapper mapper;

    public MyBatisScriptDraftRepository(ScriptDraftMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ScriptDraft draft) {
        ScriptDraftRecord r = toRecord(draft);
        if (mapper.findById(draft.id().toString()) == null) {
            mapper.insert(r);
        } else {
            mapper.update(r);
        }
    }

    @Override
    public Optional<ScriptDraft> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    @Override
    public List<ScriptDraft> findByProjectId(UUID projectId) {
        return mapper.findByProjectId(projectId.toString()).stream().map(this::toDomain).toList();
    }

    private ScriptDraft toDomain(ScriptDraftRecord r) {
        return ScriptDraft.rehydrate(
                UUID.fromString(r.getId()), UUID.fromString(r.getProjectId()),
                UUID.fromString(r.getUserId()), r.getTitle(), r.getStrategy(),
                r.getStatus(), r.getTotalScenes(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private ScriptDraftRecord toRecord(ScriptDraft d) {
        ScriptDraftRecord r = new ScriptDraftRecord();
        r.setId(d.id().toString());
        r.setProjectId(d.projectId().toString());
        r.setUserId(d.userId().toString());
        r.setTitle(d.title());
        r.setStrategy(d.strategy());
        r.setStatus(d.status());
        r.setTotalScenes(d.totalScenes());
        r.setCreatedAt(d.createdAt());
        r.setUpdatedAt(d.updatedAt());
        return r;
    }
}
