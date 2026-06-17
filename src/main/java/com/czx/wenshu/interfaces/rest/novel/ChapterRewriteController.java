package com.czx.wenshu.interfaces.rest.novel;

import com.czx.wenshu.application.novel.ChapterRewriteService;
import com.czx.wenshu.application.novel.ChapterRewriteService.RewriteMode;
import com.czx.wenshu.application.novel.ChapterRewriteService.RewriteSuggestion;
import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chapter Rewrite", description = "章节改稿建议与应用")
@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterRewriteController {

    private final ChapterRewriteService rewriteService;
    private final CurrentUserProvider currentUserProvider;

    public ChapterRewriteController(ChapterRewriteService rewriteService,
                                    CurrentUserProvider currentUserProvider) {
        this.rewriteService = rewriteService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "生成章节改稿建议")
    @PostMapping("/{chapterId}/rewrite")
    public Result<RewriteSuggestion> rewrite(@PathVariable UUID chapterId,
                                             @RequestBody RewriteRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(rewriteService.rewrite(chapterId, user.id(),
                RewriteMode.from(request.mode()), request.instruction(), request.selectedText()));
    }

    @Operation(summary = "应用章节改稿结果")
    @PostMapping("/{chapterId}/rewrite/apply")
    public Result<ChapterInfo> apply(@PathVariable UUID chapterId,
                                     @Valid @RequestBody RewriteApplyRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(rewriteService.apply(chapterId, user.id(),
                request.content(), request.acceptedChars(), request.snapshotLabel()));
    }

    public record RewriteRequest(String mode, String instruction, String selectedText) {}

    public record RewriteApplyRequest(
            @NotBlank String content,
            int acceptedChars,
            String snapshotLabel) {}
}
