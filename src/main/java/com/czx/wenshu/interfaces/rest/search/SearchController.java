package com.czx.wenshu.interfaces.rest.search;

import com.czx.wenshu.application.search.ReplaceResultInfo;
import com.czx.wenshu.application.search.SearchApplicationService;
import com.czx.wenshu.application.search.SearchResultInfo;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "全书搜索与替换")
@Validated
@RestController
@RequestMapping("/api/v1/projects/{id}")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchApplicationService searchService;
    private final CurrentUserProvider currentUserProvider;

    public SearchController(SearchApplicationService searchService,
                             CurrentUserProvider currentUserProvider) {
        this.searchService = searchService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "全书搜索",
               description = "在所有章节正文中搜索关键词，按章节分组返回命中上下文。后端执行，不在前端加载全卷。")
    @GetMapping("/search")
    public Result<SearchResultInfo> search(
            @PathVariable UUID id,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "false") boolean caseSensitive,
            @RequestParam(defaultValue = "false") boolean wholeWord) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[SearchController] 全书搜索 userId={} projectId={} keyword={}", user.id(), id, keyword);
        return Result.ok(searchService.searchChapters(id, user.id(), keyword, caseSensitive, wholeWord));
    }

    @Operation(summary = "全书替换",
               description = "替换所有章节正文中的关键词，替换前自动创建快照（auto_before_replace）。" +
                             "syncCharacterName=true 时同步更新同名角色档案（P4-06）。")
    @PostMapping("/search/replace")
    public Result<ReplaceResultInfo> replace(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceRequest request) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[SearchController] 全书替换 userId={} projectId={} keyword={} replacement={}", user.id(), id, request.keyword(), request.replacement());
        return Result.ok(searchService.replaceInChapters(
                id, user.id(),
                request.keyword(), request.replacement(),
                request.caseSensitive(), request.wholeWord(),
                request.chapterIds(),
                request.syncCharacterName()));
    }
}
