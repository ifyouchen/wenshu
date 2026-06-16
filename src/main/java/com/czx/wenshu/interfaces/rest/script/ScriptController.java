package com.czx.wenshu.interfaces.rest.script;

import com.czx.wenshu.application.script.ScriptDraftInfo;
import com.czx.wenshu.application.script.ScriptSceneInfo;
import com.czx.wenshu.application.script.ScriptService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 剧本草稿与场景控制器（P7-01）。
 * 提供草稿列表/详情、场景分页、场景编辑（乐观锁）接口。
 */
@Tag(name = "Script", description = "剧本工作台：草稿、场景（P7-01）")
@Validated
@RestController
@RequestMapping("/api/v1/script")
public class ScriptController {

    /** 剧本服务。 */
    private final ScriptService scriptService;
    /** 当前用户提供者。 */
    private final CurrentUserProvider currentUserProvider;

    public ScriptController(ScriptService scriptService,
                             CurrentUserProvider currentUserProvider) {
        this.scriptService = scriptService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "查询作品下剧本草稿列表（P7-01）",
               description = "返回指定作品的所有剧本草稿，按创建时间倒序排列。")
    @GetMapping("/projects/{projectId}/drafts")
    public Result<List<ScriptDraftInfo>> listDrafts(@PathVariable UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.listDrafts(projectId, user.id()));
    }

    @Operation(summary = "查询剧本草稿详情（P7-01）",
               description = "返回草稿元信息，包括改编策略、状态和总场景数。")
    @GetMapping("/drafts/{id}")
    public Result<ScriptDraftInfo> getDraft(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.getDraft(id, user.id()));
    }

    @Operation(summary = "分页查询草稿场景列表（P7-01）",
               description = "按场景序号正序返回，默认 page=0、size=20，最大 size=100。")
    @GetMapping("/drafts/{id}/scenes")
    public Result<ScriptService.ScenePageResult> listScenes(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.listScenes(id, user.id(), page, size));
    }

    @Operation(summary = "编辑场景内容（P7-01/P7-06）",
               description = "更新场景剧本内容，携带 version 做乐观锁校验，版本冲突返回 409。")
    @PutMapping("/scenes/{id}")
    public Result<ScriptSceneInfo> updateScene(
            @PathVariable UUID id,
            @RequestBody SceneUpdateRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.updateScene(
                id, user.id(), request.content(), request.location(),
                request.timeDesc(), request.version()));
    }

    /**
     * 场景内容更新请求体（P7-01/P7-06）。
     *
     * @param content  新剧本内容（可为 null 表示不更新）
     * @param location 场景地点
     * @param timeDesc 时间描述
     * @param version  当前版本号（乐观锁），版本不匹配返回 409
     */
    public record SceneUpdateRequest(String content, String location, String timeDesc,
                                      @NotNull int version) {}
}
