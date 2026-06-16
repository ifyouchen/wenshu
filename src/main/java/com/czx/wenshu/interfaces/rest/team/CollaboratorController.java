package com.czx.wenshu.interfaces.rest.team;

import com.czx.wenshu.application.team.TeamCollaborationService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目协作者管理接口（P9-08）。
 *
 * <ul>
 *   <li>POST   /projects/{id}/collaborators           — 添加协作者（需项目所有者）</li>
 *   <li>GET    /projects/{id}/collaborators           — 查询协作者列表</li>
 *   <li>DELETE /projects/{id}/collaborators/{userId}  — 移除协作者（需项目所有者）</li>
 * </ul>
 */
@Tag(name = "Collaborator", description = "项目协作者管理（P9-08）")
@Validated
@RestController
@RequestMapping("/api/v1/projects")
public class CollaboratorController {

    private static final Logger log = LoggerFactory.getLogger(CollaboratorController.class);

    private final TeamCollaborationService collaborationService;
    private final CurrentUserProvider currentUserProvider;

    public CollaboratorController(TeamCollaborationService collaborationService,
                                  CurrentUserProvider currentUserProvider) {
        this.collaborationService = collaborationService;
        this.currentUserProvider = currentUserProvider;
    }

    /** 添加项目协作者（需项目所有者权限）。 */
    @Operation(summary = "添加项目协作者（P9-08）")
    @PostMapping("/{projectId}/collaborators")
    public Result<TeamCollaborationService.CollaboratorInfo> addCollaborator(
            @PathVariable UUID projectId,
            @RequestBody AddCollaboratorRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[CollaboratorController] 用户 {} 添加协作者 projectId={} collaboratorId={}",
                user.id(), projectId, req.userId());
        return Result.ok(collaborationService.addCollaborator(
                projectId, user.id(), req.userId(), req.role()));
    }

    /** 查询项目协作者列表。 */
    @Operation(summary = "查询项目协作者列表（P9-08）")
    @GetMapping("/{projectId}/collaborators")
    public Result<List<TeamCollaborationService.CollaboratorInfo>> listCollaborators(
            @PathVariable UUID projectId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(collaborationService.listCollaborators(projectId, user.id()));
    }

    /** 移除项目协作者（需项目所有者权限）。 */
    @Operation(summary = "移除项目协作者（P9-08）")
    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public Result<Void> removeCollaborator(
            @PathVariable UUID projectId,
            @PathVariable UUID collaboratorId) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[CollaboratorController] 用户 {} 移除协作者 projectId={} collaboratorId={}",
                user.id(), projectId, collaboratorId);
        collaborationService.removeCollaborator(projectId, user.id(), collaboratorId);
        return Result.ok();
    }

    // ── 请求 DTO ──────────────────────────────────────────────────────────────

    /**
     * 添加协作者请求。
     *
     * @param userId 协作者用户 ID
     * @param role   协作角色（editor/viewer，默认 editor）
     */
    public record AddCollaboratorRequest(
            UUID userId,
            @Pattern(regexp = "editor|viewer", message = "角色只能是 editor 或 viewer")
            String role) {
    }
}
