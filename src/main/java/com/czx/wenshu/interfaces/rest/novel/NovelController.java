package com.czx.wenshu.interfaces.rest.novel;

import com.czx.wenshu.application.llm.StreamingLlmClient;
import com.czx.wenshu.application.novel.BranchOption;
import com.czx.wenshu.application.novel.BranchService;
import com.czx.wenshu.application.novel.ContinueContext;
import com.czx.wenshu.application.novel.ContinueService;
import com.czx.wenshu.application.novel.SkeletonApplyResult;
import com.czx.wenshu.application.novel.SkeletonInput;
import com.czx.wenshu.application.novel.SkeletonService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Novel AI", description = "AI 写作辅助：骨架生成、SSE 续写、分支建议")
@Validated
@RestController
@RequestMapping("/api/v1")
public class NovelController {

    private static final Logger log = LoggerFactory.getLogger(NovelController.class);

    /** SSE 首字超时时间（毫秒）。 */
    private static final long FIRST_TOKEN_TIMEOUT_MS = 8_000L;
    /** SSE 总连接超时（毫秒）。 */
    private static final long SSE_TOTAL_TIMEOUT_MS = 120_000L;

    private final SkeletonService skeletonService;
    private final ContinueService continueService;
    private final BranchService branchService;
    private final StreamingLlmClient streamingCreativeLlmClient;
    private final ScheduledExecutorService sseTimeoutScheduler;
    private final CurrentUserProvider currentUserProvider;

    public NovelController(SkeletonService skeletonService,
                            ContinueService continueService,
                            BranchService branchService,
                            @Qualifier("streamingCreativeLlmClient") StreamingLlmClient streamingCreativeLlmClient,
                            @Qualifier("sseTimeoutScheduler") ScheduledExecutorService sseTimeoutScheduler,
                            CurrentUserProvider currentUserProvider) {
        this.skeletonService = skeletonService;
        this.continueService = continueService;
        this.branchService = branchService;
        this.streamingCreativeLlmClient = streamingCreativeLlmClient;
        this.sseTimeoutScheduler = sseTimeoutScheduler;
        this.currentUserProvider = currentUserProvider;
    }

    // ── P5-04 骨架生成 ─────────────────────────────────────────────────────

    @Operation(summary = "提交骨架生成任务（P5-04）",
               description = "异步调用 LLM 生成故事骨架，立即返回 taskId，通过 /tasks/{taskId}/progress 轮询进度。")
    @PostMapping("/novel/skeleton")
    public Result<Map<String, String>> submitSkeleton(@Valid @RequestBody SkeletonRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[NovelController] 提交骨架生成 userId={} projectId={} targetWords={}", user.id(), request.projectId(), request.targetWords());
        String taskId = skeletonService.submitSkeletonTask(user.id(),
                new SkeletonInput(request.projectId(), request.genre(),
                        request.synopsis(), request.worldview(), request.targetWords()));
        return Result.ok(Map.of("taskId", taskId));
    }

    // ── P5-05 骨架应用入库 ─────────────────────────────────────────────────

    @Operation(summary = "应用骨架入库（P5-05）",
               description = "将已完成的骨架任务结果写入数据库，创建卷、章节和角色。")
    @PostMapping("/skeleton/{taskId}/apply")
    public Result<SkeletonApplyResult> applySkeletonTask(@PathVariable UUID taskId) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[NovelController] 应用骨架入库 userId={} taskId={}", user.id(), taskId);
        SkeletonApplyResult result = skeletonService.applySkeletonTask(taskId, user.id());
        log.info("[NovelController] 骨架入库完成 userId={} 卷数={} 章节数={} 角色数={}", user.id(), result.createdVolumes(), result.createdChapters(), result.createdCharacters());
        return Result.ok(result);
    }

    // ── P5-07 SSE 流式续写 ─────────────────────────────────────────────────

    @Operation(summary = "SSE 流式续写（P5-07）",
               description = "流式返回续写内容。事件类型：token（每个词元）、timeout（8s 无首字）、error（错误）、done（完成）。")
    @org.springframework.web.bind.annotation.GetMapping(
            value = "/novel/continue",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter continueNovel(
            @RequestParam UUID chapterId,
            @RequestParam(defaultValue = "") String instruction,
            @RequestParam(defaultValue = "500") int length) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[NovelController] SSE 续写开始 userId={} chapterId={} length={}", user.id(), chapterId, length);
        ContinueContext ctx = continueService.prepare(user.id(), chapterId, instruction, length);

        SseEmitter emitter = new SseEmitter(SSE_TOTAL_TIMEOUT_MS);
        AtomicBoolean firstTokenReceived = new AtomicBoolean(false);

        // 8 秒首字超时检测
        ScheduledFuture<?> timeoutFuture = sseTimeoutScheduler.schedule(() -> {
            if (!firstTokenReceived.get()) {
                sendEvent(emitter, "timeout", "续写首字超时（8s），请检查网络或重试");
                emitter.complete();
            }
        }, FIRST_TOKEN_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        streamingCreativeLlmClient.streamChat(
                ctx.systemPrompt(),
                ctx.userPrompt(),
                token -> {
                    if (firstTokenReceived.compareAndSet(false, true)) {
                        timeoutFuture.cancel(false);
                        log.info("[NovelController] SSE 首个 token 已接收 chapterId={}", chapterId);
                    }
                    sendEvent(emitter, "token", token);
                },
                () -> {
                    log.info("[NovelController] SSE 续写完成 chapterId={}", chapterId);
                    sendEvent(emitter, "done", "");
                    emitter.complete();
                },
                error -> {
                    log.warn("[NovelController] SSE 续写出错 chapterId={} error={}", chapterId, error.getMessage());
                    timeoutFuture.cancel(false);
                    sendEvent(emitter, "error", error.getMessage());
                    emitter.complete();
                }
        );

        return emitter;
    }

    // ── P5-08 卡点分支建议 ─────────────────────────────────────────────────

    @Operation(summary = "卡点分支建议（P5-08）",
               description = "根据当前章节和近期内容，返回多个差异化剧情发展方向。")
    @PostMapping("/novel/branch")
    public Result<List<BranchOption>> branchSuggestions(@Valid @RequestBody BranchRequest request) {
        User user = currentUserProvider.getCurrentUser();
        int count = request.branchCount() > 0 ? request.branchCount() : 3;
        log.info("[NovelController] 分支建议 userId={} chapterId={} count={}", user.id(), request.chapterId(), count);
        return Result.ok(branchService.getBranches(user.id(), request.chapterId(), count));
    }

    // ── 工具方法 ───────────────────────────────────────────────────────────

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException ignored) {
            // 客户端已断开，忽略
        }
    }
}
