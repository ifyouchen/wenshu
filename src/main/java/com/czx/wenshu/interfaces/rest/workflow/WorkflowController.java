package com.czx.wenshu.interfaces.rest.workflow;

import com.czx.wenshu.application.workflow.WorkflowInfo.Dashboard;
import com.czx.wenshu.application.workflow.WorkflowInfo.RewriteState;
import com.czx.wenshu.application.workflow.WorkflowInfo.ScriptState;
import com.czx.wenshu.application.workflow.WorkflowInfo.WriteState;
import com.czx.wenshu.application.workflow.WorkflowService;
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

@Tag(name = "Workflows", description = "三条主线页面聚合状态")
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final CurrentUserProvider currentUserProvider;

    public WorkflowController(WorkflowService workflowService,
                              CurrentUserProvider currentUserProvider) {
        this.workflowService = workflowService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "工作台聚合状态")
    @GetMapping("/dashboard")
    public Result<Dashboard> dashboard() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(workflowService.dashboard(user.id()));
    }

    @Operation(summary = "写小说流程状态")
    @GetMapping("/projects/{projectId}/write-state")
    public Result<WriteState> writeState(@PathVariable UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(workflowService.writeState(projectId, user.id()));
    }

    @Operation(summary = "改小说流程状态")
    @GetMapping("/projects/{projectId}/rewrite-state")
    public Result<RewriteState> rewriteState(@PathVariable UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(workflowService.rewriteState(projectId, user.id()));
    }

    @Operation(summary = "小说改剧本流程状态")
    @GetMapping("/projects/{projectId}/script-state")
    public Result<ScriptState> scriptState(@PathVariable UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(workflowService.scriptState(projectId, user.id()));
    }
}
