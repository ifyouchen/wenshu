package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.SummaryService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.project.ChapterSummary;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Summary", description = "章节 AI 摘要（P6-01）")
@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterSummaryController {

    private final SummaryService summaryService;
    private final CurrentUserProvider currentUserProvider;

    public ChapterSummaryController(SummaryService summaryService,
                                     CurrentUserProvider currentUserProvider) {
        this.summaryService = summaryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "触发章节摘要生成",
               description = "提交异步 LLM 摘要生成任务，返回 taskId。章节内容为空时返回 400。")
    @PostMapping("/{id}/summarize")
    public Result<Map<String, String>> submitSummary(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        String taskId = summaryService.submitSummaryTask(id, user.id());
        return Result.ok(Map.of("taskId", taskId));
    }

    @Operation(summary = "获取章节摘要",
               description = "返回已生成的章节摘要，若尚未生成则 data 为 null。")
    @GetMapping("/{id}/summary")
    public Result<Map<String, Object>> getSummary(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return summaryService.getSummary(id, user.id())
                .map(s -> Result.ok(Map.<String, Object>of(
                        "chapterId", s.chapterId().toString(),
                        "summary", s.summary(),
                        "createdAt", s.createdAt().toString())))
                .orElse(Result.ok(null));
    }
}
