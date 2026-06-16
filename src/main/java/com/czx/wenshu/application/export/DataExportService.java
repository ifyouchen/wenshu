package com.czx.wenshu.application.export;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.domain.task.AsyncTask;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户数据导出服务（P9-04）。
 *
 * <p>负责创建异步导出任务并触发后台执行。任务类型 {@code data_export}。
 * 任务完成后，前端通过 GET /tasks/{taskId}/progress 轮询结果 JSON 中的 downloadUrl。</p>
 */
@Service
public class DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

    /** 异步任务类型标识。 */
    private static final String TASK_TYPE = "data_export";

    private final AsyncTaskService asyncTaskService;
    private final DataExportTaskRunner taskRunner;

    /** 构造函数注入。 */
    public DataExportService(AsyncTaskService asyncTaskService,
                             DataExportTaskRunner taskRunner) {
        this.asyncTaskService = asyncTaskService;
        this.taskRunner = taskRunner;
    }

    /**
     * 提交数据导出任务（P9-04）。
     *
     * <p>立即返回任务 ID，实际打包和上传由 {@link DataExportTaskRunner} 异步执行。</p>
     *
     * @param userId 请求导出的用户 ID
     * @return 创建的异步任务对象（前端用 taskId 轮询进度）
     */
    public AsyncTask submitExport(UUID userId) {
        log.info("[DataExportService] 提交数据导出任务 userId={}", userId);
        AsyncTask task = asyncTaskService.createTask(userId, null, TASK_TYPE);
        taskRunner.run(task.id(), userId);
        return task;
    }
}
