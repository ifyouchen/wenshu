package com.czx.wenshu.infrastructure.persistence.consistency;

import com.czx.wenshu.domain.consistency.AiOperationLog;
import com.czx.wenshu.domain.consistency.AiOperationLogRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** MyBatis 实现的 AI 操作日志仓储（P6-06）。 */
@Repository
public class MyBatisAiOperationLogRepository implements AiOperationLogRepository {

    /** MyBatis Mapper。 */
    private final AiOperationLogMapper mapper;

    public MyBatisAiOperationLogRepository(AiOperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AiOperationLog log) {
        mapper.insert(toRecord(log));
    }

    @Override
    public Optional<AiOperationLog> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    private AiOperationLog toDomain(AiOperationLogRecord r) {
        return AiOperationLog.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getUserId()),
                r.getProjectId() != null ? UUID.fromString(r.getProjectId()) : null,
                r.getOperation(), r.getModel(), r.getCreatedAt());
    }

    private AiOperationLogRecord toRecord(AiOperationLog l) {
        AiOperationLogRecord r = new AiOperationLogRecord();
        r.setId(l.id().toString());
        r.setUserId(l.userId().toString());
        r.setProjectId(l.projectId() != null ? l.projectId().toString() : null);
        r.setOperation(l.operation());
        r.setModel(l.model());
        r.setCreatedAt(l.createdAt());
        return r;
    }
}
