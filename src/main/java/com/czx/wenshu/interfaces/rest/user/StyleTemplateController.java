package com.czx.wenshu.interfaces.rest.user;

import com.czx.wenshu.application.user.CreateStyleTemplateCommand;
import com.czx.wenshu.application.user.StyleTemplateInfo;
import com.czx.wenshu.application.user.StyleTemplateService;
import com.czx.wenshu.application.user.UpdateStyleTemplateCommand;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "StyleTemplates", description = "写作/润色风格模板")
@RestController
@RequestMapping("/api/v1/style-templates")
public class StyleTemplateController {

    private final StyleTemplateService styleTemplateService;
    private final CurrentUserProvider currentUserProvider;

    public StyleTemplateController(StyleTemplateService styleTemplateService,
                                   CurrentUserProvider currentUserProvider) {
        this.styleTemplateService = styleTemplateService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "风格模板列表", description = "按当前用户查询写作/润色风格模板，可用 type=writing|polish 筛选。")
    @GetMapping
    public Result<List<StyleTemplateInfo>> list(@RequestParam(required = false) String type) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleTemplateService.list(user.id(), type));
    }

    @Operation(summary = "创建风格模板")
    @PostMapping
    public Result<StyleTemplateInfo> create(@RequestBody StyleTemplateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleTemplateService.create(user.id(), new CreateStyleTemplateCommand(
                request.name(), request.templateType(), request.genres(), request.prompt())));
    }

    @Operation(summary = "更新风格模板")
    @PutMapping("/{id}")
    public Result<StyleTemplateInfo> update(@PathVariable UUID id, @RequestBody StyleTemplateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleTemplateService.update(id, user.id(), new UpdateStyleTemplateCommand(
                request.name(), request.templateType(), request.genres(), request.prompt())));
    }

    @Operation(summary = "激活风格模板", description = "同一类型下只保留一个激活模板。")
    @PutMapping("/{id}/activate")
    public Result<StyleTemplateInfo> activate(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(styleTemplateService.activate(id, user.id()));
    }

    @Operation(summary = "删除风格模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        styleTemplateService.delete(id, user.id());
        return Result.ok();
    }
}
