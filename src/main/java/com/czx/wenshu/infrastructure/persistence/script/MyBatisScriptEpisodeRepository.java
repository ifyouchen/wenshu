package com.czx.wenshu.infrastructure.persistence.script;

import com.czx.wenshu.domain.script.ScriptEpisode;
import com.czx.wenshu.domain.script.ScriptEpisodeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的剧本集数仓储（P7-07）。 */
@Repository
public class MyBatisScriptEpisodeRepository implements ScriptEpisodeRepository {

    private final ScriptEpisodeMapper mapper;

    public MyBatisScriptEpisodeRepository(ScriptEpisodeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ScriptEpisode episode) {
        mapper.insert(toRecord(episode));
    }

    @Override
    public List<ScriptEpisode> findByDraftId(UUID draftId) {
        return mapper.findByDraftId(draftId.toString()).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        mapper.deleteById(id.toString());
    }

    private ScriptEpisode toDomain(ScriptEpisodeRecord r) {
        return ScriptEpisode.rehydrate(UUID.fromString(r.getId()), UUID.fromString(r.getDraftId()),
                r.getEpisodeNo(), r.getTitle(), r.getSortOrder(), r.getCreatedAt());
    }

    private ScriptEpisodeRecord toRecord(ScriptEpisode e) {
        ScriptEpisodeRecord r = new ScriptEpisodeRecord();
        r.setId(e.id().toString());
        r.setDraftId(e.draftId().toString());
        r.setEpisodeNo(e.episodeNo());
        r.setTitle(e.title());
        r.setSortOrder(e.sortOrder());
        r.setCreatedAt(e.createdAt());
        return r;
    }
}
