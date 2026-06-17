package com.czx.wenshu.interfaces.rest.novel;

import com.czx.wenshu.application.novel.StoryToolInfo;
import com.czx.wenshu.application.novel.StoryToolResult;
import com.czx.wenshu.application.novel.StoryToolService;
import com.czx.wenshu.common.exception.ApiException;
import com.czx.wenshu.common.result.ErrorCode;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story Tools", description = "网文工具箱：写作、拆文、扫榜、导入、封面、去AI味、审查、章节提取")
@Validated
@RestController
@RequestMapping("/api/v1/story-tools")
public class StoryToolController {

    private static final Logger log = LoggerFactory.getLogger(StoryToolController.class);

    private final StoryToolService storyToolService;
    private final CurrentUserProvider currentUserProvider;

    public StoryToolController(StoryToolService storyToolService, CurrentUserProvider currentUserProvider) {
        this.storyToolService = storyToolService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "列出可用网文工具")
    @GetMapping
    public Result<List<StoryToolInfo>> listTools() {
        return Result.ok(storyToolService.listTools());
    }

    @Operation(summary = "运行网文工具",
               description = "tool 使用 /story-tools 返回的 id，例如 story-long-write、story-short-analyze、story-cover、story-deslop。")
    @PostMapping("/{tool}/run")
    public Result<StoryToolResult> runTool(@PathVariable String tool,
                                           @RequestBody(required = false) StoryToolRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "请求体不能为空");
        }
        User user = currentUserProvider.getCurrentUser();
        log.info("[StoryToolController] 运行工具 userId={} tool={} projectId={} chapterId={}",
                user.id(), tool, request.projectId(), request.chapterId());
        return Result.ok(storyToolService.runTool(
                user.id(),
                tool,
                request.projectId(),
                request.chapterId(),
                request.input(),
                request.instruction(),
                request.targetWords()
        ));
    }
}
