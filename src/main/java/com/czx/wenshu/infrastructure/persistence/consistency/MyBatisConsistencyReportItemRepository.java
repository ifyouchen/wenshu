package com.czx.wenshu.infrastructure.persistence.consistency;

import com.czx.wenshu.domain.consistency.ConsistencyReportItem;
import com.czx.wenshu.domain.consistency.ConsistencyReportItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的一致性审查条目仓储（P6-06/P6-07）。 */
@Repository
public class MyBatisConsistencyReportItemRepository implements ConsistencyReportItemRepository {

    /** MyBatis Mapper。 */
    private final ConsistencyReportItemMapper mapper;

    public MyBatisConsistencyReportItemRepository(ConsistencyReportItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ConsistencyReportItem item) {
        if (mapper.findById(item.id().toString()) == null) {
            mapper.insert(toRecord(item));
        } else {
            ConsistencyReportItemRecord r = new ConsistencyReportItemRecord();
            r.setId(item.id().toString());
            r.setStatus(item.status());
            r.setUpdatedAt(item.updatedAt());
            mapper.updateStatus(r);
        }
    }

    @Override
    public List<ConsistencyReportItem> findByReportId(UUID reportId) {
        return mapper.findByReportId(reportId.toString()).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public Optional<ConsistencyReportItem> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    private ConsistencyReportItem toDomain(ConsistencyReportItemRecord r) {
        return ConsistencyReportItem.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getReportId()),
                UUID.fromString(r.getProjectId()),
                r.getType(), r.getCharacter(), r.getChapterHint(),
                r.getDescription(), r.getSuggestion(), r.getStatus(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    private ConsistencyReportItemRecord toRecord(ConsistencyReportItem item) {
        ConsistencyReportItemRecord r = new ConsistencyReportItemRecord();
        r.setId(item.id().toString());
        r.setReportId(item.reportId().toString());
        r.setProjectId(item.projectId().toString());
        r.setType(item.type());
        r.setCharacter(item.character());
        r.setChapterHint(item.chapterHint());
        r.setDescription(item.description());
        r.setSuggestion(item.suggestion());
        r.setStatus(item.status());
        r.setCreatedAt(item.createdAt());
        r.setUpdatedAt(item.updatedAt());
        return r;
    }
}
