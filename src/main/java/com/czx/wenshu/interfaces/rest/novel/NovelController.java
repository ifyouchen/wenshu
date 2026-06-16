package com.czx.wenshu.interfaces.rest.novel;

import com.czx.wenshu.application.novel.SkeletonApplyResult;
import com.czx.wenshu.application.novel.SkeletonInput;
import com.czx.wenshu.application.novel.SkeletonService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Novel AI", description = "AI 写作辅助：骨架生成、应用骨架")
@Validated
@RestController
@RequestMapping("/api/v1")
public class NovelController {

    private final SkeletonService skeletonService;
    private final CurrentUserProvider currentUserProvider;

    public NovelController(SkeletonService skeletonService, CurrentUserProvider currentUserProvider) {
        this.skeletonService = skeletonService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "提交骨架生成任务（P5-04）",
               description = "异步调用 LLM 生成故事骨架，立即返回 taskId，通过 /tasks/{taskId}/progress 轮询进度。")
    @PostMapping("/novel/skeleton")
    public Result<Map<String, String>> submitSkeleton(@Valid @RequestBody SkeletonRequest request) {
        User user = currentUserProvider.getCurrentUser();
        String taskId = skeletonService.submitSkeletonTask(user.id(),
                new SkeletonInput(request.projectId(), request.genre(),
                        request.synopsis(), request.worldview(), request.targetWords()));
        return Result.ok(Map.of("taskId", taskId));
    }

    @Operation(summary = "应用骨架入库（P5-05）",
               description = "将已完成的骨架任务结果写入数据库，创建卷、章节和角色。")
    @PostMapping("/skeleton/{taskId}/apply")
    public Result<SkeletonApplyResult> applySkeletonTask(@PathVariable UUID taskId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(skeletonService.applySkeletonTask(taskId, user.id()));
    }
}
