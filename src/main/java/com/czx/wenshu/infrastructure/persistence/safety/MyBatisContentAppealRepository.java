package com.czx.wenshu.infrastructure.persistence.safety;

import com.czx.wenshu.domain.safety.ContentAppeal;
import com.czx.wenshu.domain.safety.ContentAppealRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * {@link ContentAppealRepository} 的 MyBatis 实现（P9-05）。
 */
@Repository
public class MyBatisContentAppealRepository implements ContentAppealRepository {

    private final ContentAppealMapper mapper;

    public MyBatisContentAppealRepository(ContentAppealMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(ContentAppeal appeal) {
        ContentAppealRecord rec = toRecord(appeal);
        mapper.insert(rec);
    }

    @Override
    public List<ContentAppeal> findByUserId(UUID userId) {
        return mapper.findByUserId(userId.toString()).stream()
                .map(this::toDomain).toList();
    }

    /** 领域对象转持久化记录。 */
    private ContentAppealRecord toRecord(ContentAppeal appeal) {
        ContentAppealRecord rec = new ContentAppealRecord();
        rec.setId(appeal.id().toString());
        rec.setUserId(appeal.userId().toString());
        rec.setContent(appeal.content());
        rec.setReason(appeal.reason());
        rec.setStatus(appeal.status());
        rec.setReviewerNote(appeal.reviewerNote());
        rec.setCreatedAt(appeal.createdAt());
        rec.setUpdatedAt(appeal.updatedAt());
        return rec;
    }

    /** 持久化记录转领域对象。 */
    private ContentAppeal toDomain(ContentAppealRecord rec) {
        return ContentAppeal.rehydrate(
                UUID.fromString(rec.getId()),
                UUID.fromString(rec.getUserId()),
                rec.getContent(),
                rec.getReason(),
                rec.getStatus(),
                rec.getReviewerNote(),
                rec.getCreatedAt(),
                rec.getUpdatedAt()
        );
    }
}
