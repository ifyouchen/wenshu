package com.czx.wenshu.application.task;

import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.domain.task.AsyncTask;
import com.czx.wenshu.domain.task.AsyncTaskRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 异步任务生命周期管理（P5-03）。 */
@Service
public class AsyncTaskService {

    private final AsyncTaskRepository taskRepository;
    private final Clock clock;

    public AsyncTaskService(AsyncTaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    /** 创建新任务（状态 PENDING）并持久化，返回任务实体。 */
    @Transactional
    public AsyncTask createTask(UUID userId, UUID projectId, String taskType) {
        AsyncTask task = AsyncTask.create(userId, projectId, taskType, clock);
        taskRepository.save(task);
        return task;
    }

    /** 将任务标记为 RUNNING 并记录总步骤数。 */
    @Transactional
    public AsyncTask markRunning(UUID taskId, int totalSteps, String stepLabel) {
        AsyncTask task = loadTask(taskId);
        task.markRunning(totalSteps, stepLabel, clock);
        taskRepository.save(task);
        return task;
    }

    /** 更新任务进度（currentStep、stepLabel、progressPct）。 */
    @Transactional
    public AsyncTask updateProgress(UUID taskId, int currentStep, String stepLabel, int progressPct) {
        AsyncTask task = loadTask(taskId);
        task.updateProgress(currentStep, stepLabel, progressPct, clock);
        taskRepository.save(task);
        return task;
    }

    /** 将任务标记为 COMPLETED，记录结果 ID。 */
    @Transactional
    public AsyncTask complete(UUID taskId, UUID resultId) {
        AsyncTask task = loadTask(taskId);
        task.complete(resultId, clock);
        taskRepository.save(task);
        return task;
    }

    /** 将任务标记为 COMPLETED，存储 JSON 结果（用于骨架生成等 LLM 任务，P5-04）。 */
    @Transactional
    public AsyncTask completeWithJson(UUID taskId, String resultJson) {
        AsyncTask task = loadTask(taskId);
        task.completeWithJson(resultJson, clock);
        taskRepository.save(task);
        return task;
    }

    /** 将任务标记为 FAILED，记录错误信息。 */
    @Transactional
    public AsyncTask fail(UUID taskId, String errorMessage) {
        AsyncTask task = loadTask(taskId);
        task.fail(errorMessage, clock);
        taskRepository.save(task);
        return task;
    }

    /** 查询任务进度，权限校验（任务必须属于当前用户）。 */
    @Transactional(readOnly = true)
    public TaskProgressInfo getProgress(UUID taskId, UUID userId) {
        AsyncTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.userId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该任务");
        }
        return TaskProgressInfo.from(task);
    }

    private AsyncTask loadTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "任务不存在"));
    }
}
