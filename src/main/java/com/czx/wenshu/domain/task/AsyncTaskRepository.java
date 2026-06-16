package com.czx.wenshu.domain.task;

import java.util.Optional;
import java.util.UUID;

public interface AsyncTaskRepository {

    void save(AsyncTask task);

    Optional<AsyncTask> findById(UUID id);
}
