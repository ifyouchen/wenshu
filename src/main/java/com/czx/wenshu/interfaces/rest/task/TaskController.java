package com.czx.wenshu.interfaces.rest.task;

import com.czx.wenshu.application.task.AsyncTaskService;
import com.czx.wenshu.application.task.TaskProgressInfo;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tasks", description = "异步任务进度查询")
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final AsyncTaskService asyncTaskService;
    private final CurrentUserProvider currentUserProvider;

    public TaskController(AsyncTaskService asyncTaskService, CurrentUserProvider currentUserProvider) {
        this.asyncTaskService = asyncTaskService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "查询异步任务进度",
               description = "轮询任务状态（pending/running/completed/failed）及进度百分比。")
    @GetMapping("/{taskId}/progress")
    public Result<TaskProgressInfo> getProgress(@PathVariable UUID taskId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(asyncTaskService.getProgress(taskId, user.id()));
    }
}
