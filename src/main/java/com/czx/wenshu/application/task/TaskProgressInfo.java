package com.czx.wenshu.application.task;

import com.czx.wenshu.domain.task.AsyncTask;

/** 异步任务进度响应 DTO（P5-03）。 */
public record TaskProgressInfo(
        String taskId,
        String taskType,
        String status,
        int currentStep,
        Integer totalSteps,
        String stepLabel,
        int progressPct,
        String resultId,
        String resultJson,
        String errorMessage,
        String createdAt,
        String updatedAt) {

    public static TaskProgressInfo from(AsyncTask task) {
        return new TaskProgressInfo(
                task.id().toString(),
                task.taskType(),
                task.status().value(),
                task.currentStep(),
                task.totalSteps(),
                task.stepLabel(),
                task.progressPct(),
                task.resultId() != null ? task.resultId().toString() : null,
                task.resultJson(),
                task.errorMessage(),
                task.createdAt().toString(),
                task.updatedAt().toString()
        );
    }
}
