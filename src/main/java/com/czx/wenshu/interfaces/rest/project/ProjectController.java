package com.czx.wenshu.interfaces.rest.project;

import com.czx.wenshu.application.project.CreateChapterCommand;
import com.czx.wenshu.application.project.CreateProjectCommand;
import com.czx.wenshu.application.project.CreateVolumeCommand;
import com.czx.wenshu.application.project.ProjectApplicationService;
import com.czx.wenshu.application.project.ProjectInfo;
import com.czx.wenshu.application.project.UpdateChapterCommand;
import com.czx.wenshu.application.project.UpdateProjectCommand;
import com.czx.wenshu.application.project.UpdateVolumeCommand;
import com.czx.wenshu.application.project.VolumeInfo;
import com.czx.wenshu.application.project.ChapterInfo;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.project.ChapterStatus;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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

@Tag(name = "Projects", description = "作品、卷与章节")
@Validated
@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectApplicationService projectService;
    private final CurrentUserProvider currentUserProvider;

    public ProjectController(ProjectApplicationService projectService, CurrentUserProvider currentUserProvider) {
        this.projectService = projectService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "作品列表", description = "返回当前用户的所有作品。")
    @GetMapping("/projects")
    public Result<List<ProjectInfo>> listProjects() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.listProjects(user.id()));
    }

    @Operation(summary = "创建作品", description = "创建新作品。")
    @PostMapping("/projects")
    public Result<ProjectInfo> createProject(@Valid @RequestBody CreateProjectRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.createProject(user.id(), new CreateProjectCommand(
                request.title(), request.genre(), request.synopsis(), request.worldview())));
    }

    @Operation(summary = "作品详情", description = "获取单个作品详情。")
    @GetMapping("/projects/{id}")
    public Result<ProjectInfo> getProject(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.getProject(id, user.id()));
    }

    @Operation(summary = "更新作品", description = "更新作品信息。")
    @PutMapping("/projects/{id}")
    public Result<ProjectInfo> updateProject(@PathVariable UUID id, @RequestBody UpdateProjectRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.updateProject(id, user.id(), new UpdateProjectCommand(
                request.title(), request.genre(), request.synopsis(), request.worldview())));
    }

    @Operation(summary = "删除作品", description = "删除作品及其所有卷章，需要 confirm=true。")
    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.fail(com.czx.wenshu.common.result.ErrorCode.BAD_REQUEST, "请确认删除操作");
        }
        User user = currentUserProvider.getCurrentUser();
        projectService.deleteProject(id, user.id());
        return Result.ok();
    }

    @Operation(summary = "新增卷", description = "在指定作品下新增卷。")
    @PostMapping("/projects/{id}/volumes")
    public Result<VolumeInfo> createVolume(@PathVariable UUID id, @Valid @RequestBody CreateVolumeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.createVolume(id, user.id(), new CreateVolumeCommand(
                request.title(), request.conflict(), request.sortOrder())));
    }

    @Operation(summary = "更新卷", description = "更新卷信息。")
    @PutMapping("/volumes/{id}")
    public Result<VolumeInfo> updateVolume(@PathVariable UUID id, @RequestBody UpdateVolumeRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.updateVolume(id, user.id(), new UpdateVolumeCommand(
                request.title(), request.conflict())));
    }

    @Operation(summary = "删除卷", description = "删除卷及其所有章节，需要 confirm=true。")
    @DeleteMapping("/volumes/{id}")
    public Result<Void> deleteVolume(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return Result.fail(com.czx.wenshu.common.result.ErrorCode.BAD_REQUEST, "请确认删除操作");
        }
        User user = currentUserProvider.getCurrentUser();
        projectService.deleteVolume(id, user.id());
        return Result.ok();
    }

    @Operation(summary = "新增章节", description = "在指定卷下新增章节。")
    @PostMapping("/volumes/{id}/chapters")
    public Result<ChapterInfo> createChapter(@PathVariable UUID id, @Valid @RequestBody CreateChapterRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.createChapter(id, user.id(), new CreateChapterCommand(
                request.title(), request.outline(), request.sortOrder())));
    }

    @Operation(summary = "章节详情", description = "获取章节详情，包含内容。")
    @GetMapping("/chapters/{id}")
    public Result<ChapterInfo> getChapter(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.getChapter(id, user.id()));
    }

    @Operation(summary = "保存章节", description = "保存章节内容、标题、状态、大纲。")
    @PutMapping("/chapters/{id}")
    public Result<ChapterInfo> updateChapter(@PathVariable UUID id, @RequestBody UpdateChapterRequest request) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(projectService.updateChapter(id, user.id(), new UpdateChapterCommand(
                request.title(), request.content(), request.outline(), request.status())));
    }

    @Operation(summary = "删除章节", description = "删除章节。")
    @DeleteMapping("/chapters/{id}")
    public Result<Void> deleteChapter(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        projectService.deleteChapter(id, user.id());
        return Result.ok();
    }
}