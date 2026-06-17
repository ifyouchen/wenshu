package com.czx.wenshu.interfaces.rest.script;

import com.czx.wenshu.application.script.ScriptConversionService;
import com.czx.wenshu.application.script.ScriptDraftInfo;
import com.czx.wenshu.application.script.ScriptEpisodeInfo;
import com.czx.wenshu.application.script.ScriptSceneInfo;
import com.czx.wenshu.application.script.ScriptService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /** 剧本服务（草稿/场景/集数/导出）。 */
    private final ScriptService scriptService;
    /** 剧本改编提交服务（P7-04）。 */
    private final ScriptConversionService conversionService;
    /** 当前用户提供者。 */
    private final CurrentUserProvider currentUserProvider;

    public ScriptController(ScriptService scriptService,
                             ScriptConversionService conversionService,
                             CurrentUserProvider currentUserProvider) {
        this.scriptService = scriptService;
        this.conversionService = conversionService;
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

    // ── P7-04 异步剧本改编 ─────────────────────────────────────────────────

    @Operation(summary = "提交剧本改编任务（P7-04）",
               description = "异步将小说章节转换为剧本场景，消耗一次改编配额，返回 taskId 和 draftId。")
    @PostMapping("/convert")
    public Result<Map<String, String>> convert(@Valid @RequestBody ConvertRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(conversionService.submitConversion(
                request.projectId(), user.id(), request.title(), request.psychologyStrategy(), request.chapterIds()));
    }

    // ── P7-07 分集管理 ─────────────────────────────────────────────────────

    @Operation(summary = "创建集数（P7-07）",
               description = "在草稿中创建新集，集序号从 1 开始。")
    @PostMapping("/drafts/{id}/episodes")
    public Result<ScriptEpisodeInfo> createEpisode(
            @PathVariable UUID id,
            @RequestBody EpisodeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.createEpisode(id, user.id(),
                request.title(), request.episodeNo()));
    }

    @Operation(summary = "查询集数列表（P7-07）",
               description = "返回草稿下所有集数，按排序正序。")
    @GetMapping("/drafts/{id}/episodes")
    public Result<List<ScriptEpisodeInfo>> listEpisodes(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.listEpisodes(id, user.id()));
    }

    @Operation(summary = "删除集数（P7-07）",
               description = "删除指定集数，场景归属不受影响（episode_id 变为 null）。")
    @DeleteMapping("/drafts/{draftId}/episodes/{episodeId}")
    public Result<Void> deleteEpisode(@PathVariable UUID draftId, @PathVariable UUID episodeId) {
        User user = currentUserProvider.getCurrentUser();
        scriptService.deleteEpisode(draftId, episodeId, user.id());
        return Result.ok();
    }

    // ── P7-08 导出 ─────────────────────────────────────────────────────────

    @Operation(summary = "提交剧本导出任务（P7-08）",
               description = "异步生成 DOCX/FDX/分镜文件并上传至 COS，完成后通过 /tasks/{taskId}/progress 获取预签名 URL。" +
                             "当前支持格式：docx（默认）、fdx、storyboard。")
    @PostMapping("/drafts/{id}/export")
    public Result<Map<String, String>> export(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "docx") String format) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(scriptService.submitExport(id, user.id(), format));
    }

    // ── 请求体内部记录 ─────────────────────────────────────────────────────

    /**
     * 剧本改编提交请求体（P7-04）。
     *
     * @param projectId          作品 ID
     * @param title              草稿标题（可省略）
     * @param psychologyStrategy 心理外化策略（action/dialogue/voiceover/skip）
     * @param chapterIds         改编章节范围，空表示整部作品
     */
    public record ConvertRequest(
            @NotNull UUID projectId,
            String title,
            String psychologyStrategy,
            List<UUID> chapterIds) {}

    /**
     * 集数创建请求体（P7-07）。
     *
     * @param episodeNo 集序号（从 1 开始）
     * @param title     集标题
     */
    public record EpisodeRequest(int episodeNo, @NotBlank String title) {}

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
