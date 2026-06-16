package com.czx.wenshu.interfaces.rest.team;

import com.czx.wenshu.application.team.MemberInfo;
import com.czx.wenshu.application.team.TeamInfo;
import com.czx.wenshu.application.team.TeamService;
import com.czx.wenshu.common.result.Result;
import com.czx.wenshu.domain.user.User;
import com.czx.wenshu.interfaces.rest.auth.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 团队版成员管理接口（P9-07）。
 *
 * <ul>
 *   <li>POST  /teams             — 创建团队</li>
 *   <li>GET   /teams             — 当前用户所在团队列表</li>
 *   <li>POST  /teams/{id}/invites — 邀请成员</li>
 *   <li>POST  /teams/invites/{code}/accept — 接受邀请</li>
 *   <li>GET   /teams/{id}/members — 查询成员列表</li>
 *   <li>DELETE /teams/{id}/members/{userId} — 移除成员</li>
 *   <li>PUT   /teams/{id}/members/{userId}/role — 修改角色</li>
 * </ul>
 */
@Tag(name = "Team", description = "团队版成员管理与共享配额")
@Validated
@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private static final Logger log = LoggerFactory.getLogger(TeamController.class);

    private final TeamService teamService;
    private final CurrentUserProvider currentUserProvider;

    public TeamController(TeamService teamService, CurrentUserProvider currentUserProvider) {
        this.teamService = teamService;
        this.currentUserProvider = currentUserProvider;
    }

    /** 创建团队。 */
    @Operation(summary = "创建团队（P9-07）", description = "创建者自动成为 admin 成员。")
    @PostMapping
    public Result<TeamInfo> createTeam(@Valid @RequestBody CreateTeamRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[TeamController] 用户 {} 创建团队 name={}", user.id(), req.name());
        return Result.ok(teamService.createTeam(user.id(), req.name()));
    }

    /** 查询当前用户所在的所有团队。 */
    @Operation(summary = "查询我的团队列表（P9-07）")
    @GetMapping
    public Result<List<TeamInfo>> listMyTeams() {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(teamService.listMyTeams(user.id()));
    }

    /** 邀请成员加入团队（需 admin 权限）。 */
    @Operation(summary = "邀请成员（P9-07）", description = "返回邀请码，将其发送给受邀用户。")
    @PostMapping("/{teamId}/invites")
    public Result<MemberInfo> inviteMember(@PathVariable UUID teamId,
                                           @Valid @RequestBody InviteRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[TeamController] 用户 {} 邀请成员 inviteeId={}", user.id(), req.userId());
        return Result.ok(teamService.invite(teamId, user.id(), req.userId()));
    }

    /** 接受邀请（受邀用户调用）。 */
    @Operation(summary = "接受邀请（P9-07）")
    @PostMapping("/invites/{inviteCode}/accept")
    public Result<MemberInfo> acceptInvite(@PathVariable String inviteCode) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[TeamController] 用户 {} 接受邀请 inviteCode={}", user.id(), inviteCode);
        return Result.ok(teamService.acceptInvite(inviteCode, user.id()));
    }

    /** 查询团队成员列表。 */
    @Operation(summary = "查询团队成员列表（P9-07）")
    @GetMapping("/{teamId}/members")
    public Result<List<MemberInfo>> listMembers(@PathVariable UUID teamId) {
        User user = currentUserProvider.getCurrentUser();
        return Result.ok(teamService.listMembers(teamId, user.id()));
    }

    /** 移除团队成员（需 admin 权限）。 */
    @Operation(summary = "移除团队成员（P9-07）")
    @DeleteMapping("/{teamId}/members/{memberId}")
    public Result<Void> removeMember(@PathVariable UUID teamId,
                                     @PathVariable UUID memberId) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[TeamController] 用户 {} 移除成员 memberId={}", user.id(), memberId);
        teamService.removeMember(teamId, user.id(), memberId);
        return Result.ok();
    }

    /** 修改成员角色（需 admin 权限）。 */
    @Operation(summary = "修改成员角色（P9-07）")
    @PutMapping("/{teamId}/members/{memberId}/role")
    public Result<Void> changeRole(@PathVariable UUID teamId,
                                   @PathVariable UUID memberId,
                                   @Valid @RequestBody RoleRequest req) {
        User user = currentUserProvider.getCurrentUser();
        log.info("[TeamController] 用户 {} 修改角色 memberId={} newRole={}", user.id(), memberId, req.role());
        teamService.changeRole(teamId, user.id(), memberId, req.role());
        return Result.ok();
    }

    // ── 请求 DTO ──────────────────────────────────────────────────────────────

    /** 创建团队请求。 */
    public record CreateTeamRequest(
            @NotBlank(message = "团队名称不能为空") String name) {
    }

    /** 邀请成员请求。 */
    public record InviteRequest(UUID userId) {
    }

    /** 修改角色请求。 */
    public record RoleRequest(
            @NotBlank
            @Pattern(regexp = "admin|member", message = "角色只能是 admin 或 member")
            String role) {
    }
}
