package com.czx.wenshu.infrastructure.persistence.task;

import com.czx.wenshu.domain.task.AsyncTask;
import com.czx.wenshu.domain.task.AsyncTaskRepository;
import com.czx.wenshu.domain.task.AsyncTaskStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAsyncTaskRepository implements AsyncTaskRepository {

    private final AsyncTaskMapper mapper;

    public MyBatisAsyncTaskRepository(AsyncTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(AsyncTask task) {
        AsyncTaskRecord record = toRecord(task);
        if (mapper.findById(task.id().toString()) == null) {
            mapper.insert(record);
        } else {
            mapper.update(record);
        }
    }

    @Override
    public Optional<AsyncTask> findById(UUID id) {
        return Optional.ofNullable(mapper.findById(id.toString())).map(this::toDomain);
    }

    private AsyncTask toDomain(AsyncTaskRecord r) {
        return AsyncTask.rehydrate(
                UUID.fromString(r.getId()),
                UUID.fromString(r.getUserId()),
                r.getProjectId() != null ? UUID.fromString(r.getProjectId()) : null,
                r.getTaskType(),
                AsyncTaskStatus.fromValue(r.getStatus()),
                r.getCurrentStep(),
                r.getTotalSteps(),
                r.getStepLabel(),
                r.getProgressPct(),
                r.getResultId() != null ? UUID.fromString(r.getResultId()) : null,
                r.getResultJson(),
                r.getErrorMessage(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private AsyncTaskRecord toRecord(AsyncTask t) {
        AsyncTaskRecord r = new AsyncTaskRecord();
        r.setId(t.id().toString());
        r.setUserId(t.userId().toString());
        r.setProjectId(t.projectId() != null ? t.projectId().toString() : null);
        r.setTaskType(t.taskType());
        r.setStatus(t.status().value());
        r.setCurrentStep(t.currentStep());
        r.setTotalSteps(t.totalSteps());
        r.setStepLabel(t.stepLabel());
        r.setProgressPct(t.progressPct());
        r.setResultId(t.resultId() != null ? t.resultId().toString() : null);
        r.setResultJson(t.resultJson());
        r.setErrorMessage(t.errorMessage());
        r.setCreatedAt(t.createdAt());
        r.setUpdatedAt(t.updatedAt());
        return r;
    }
}
