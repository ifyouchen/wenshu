package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.ChapterContextInfo;
import com.czx.wenshu.application.project.ChapterContextService;
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

@Tag(name = "ChapterContext", description = "编辑器章节上下文聚合")
@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterContextController {

    private final ChapterContextService chapterContextService;
    private final CurrentUserProvider currentUserProvider;

    public ChapterContextController(ChapterContextService chapterContextService,
                                    CurrentUserProvider currentUserProvider) {
        this.chapterContextService = chapterContextService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "章节上下文", description = "聚合章节、项目角色、世界观词典和章节关键事件，供编辑器侧栏一次加载。")
    @GetMapping("/{id}/context")
    public Result<ChapterContextInfo> getContext(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(chapterContextService.getContext(id, user.id()));
    }
}
